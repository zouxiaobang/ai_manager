package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.storage.DualWriteSaveOutcome;
import com.ai.manager.system.domain.storage.NoteContentMeta;
import com.ai.manager.system.domain.storage.NoteContentReconcileResult;
import com.ai.manager.system.domain.storage.NoteContentRef;
import com.ai.manager.system.domain.storage.NoteContentSaveResult;
import com.ai.manager.system.mapper.NbNoteMapper;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.NbNoteContentService;
import com.ai.manager.system.service.NoteContentVersionService;
import com.ai.manager.system.service.StorageCenterService;
import com.ai.manager.system.service.storage.BaiduPanNoteContentStorage;
import com.ai.manager.system.service.storage.DualWriteNoteContentStorage;
import com.ai.manager.system.service.storage.LocalFileNoteContentStorage;
import com.ai.manager.system.service.storage.NoteContentStorage;
import com.ai.manager.system.service.support.NoteSyncStatus;
import com.ai.manager.system.util.NoteContentUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NbNoteContentServiceImpl implements NbNoteContentService {

    private static final String CACHE_PREFIX = "nb:content:";
    private static final int MAX_SYNC_RETRY_ROUNDS = 3;

    private final NoteStorageProperties noteStorageProperties;
    private final BaiduPanAuthService baiduPanAuthService;
    private final BaiduPanNoteContentStorage baiduPanNoteContentStorage;
    private final LocalFileNoteContentStorage localFileNoteContentStorage;
    private final DualWriteNoteContentStorage dualWriteNoteContentStorage;
    private final StringRedisTemplate stringRedisTemplate;
    private final NbNoteMapper nbNoteMapper;
    private final StorageCenterService storageCenterService;
    private final NoteContentVersionService noteContentVersionService;

    private Map<String, NoteContentStorage> storageMap;

    @jakarta.annotation.PostConstruct
    void init() {
        List<NoteContentStorage> storages = List.of(
                dualWriteNoteContentStorage,
                baiduPanNoteContentStorage,
                localFileNoteContentStorage
        );
        storageMap = storages.stream().collect(Collectors.toMap(NoteContentStorage::type, Function.identity()));
        localFileNoteContentStorage.ensureRoot();
    }

    @Override
    public String loadContent(NbNote note) {
        if (note == null || note.getId() == null) {
            return "";
        }
        if (storageCenterService.isDualStorageEnabled()) {
            NoteContentRef ref = toRef(note);
            NoteContentReconcileResult result = noteContentVersionService.reconcile(note, ref);
            applyReconcileResult(note, result);
            cacheContent(note.getId(), result.getContent());
            return result.getContent();
        }

        String cacheKey = CACHE_PREFIX + note.getId();
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (NoteContentUtils.isBaiduApiErrorBody(cached)) {
                stringRedisTemplate.delete(cacheKey);
            } else {
                return cached;
            }
        }
        NoteContentStorage storage = resolveStorage(note);
        String content = storage.load(toRef(note));
        if (NoteContentUtils.isBaiduApiErrorBody(content)) {
            throw new IllegalStateException("笔记正文下载异常，请稍后重试");
        }
        cacheContent(note.getId(), content);
        return content;
    }

    @Override
    public void stageContent(NbNote note, String content) {
        if (note == null || note.getId() == null) {
            return;
        }
        String normalized = content == null ? "" : content;
        String cacheKey = CACHE_PREFIX + note.getId();
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (normalized.equals(cached)) {
            String pendingHash = NoteContentUtils.sha256(normalized);
            if (pendingHash.equals(note.getContentHash()) && NoteSyncStatus.SYNCED.equals(note.getSyncStatus())) {
                return;
            }
        }
        cacheContent(note.getId(), normalized);
        note.setContentExcerpt(NoteContentUtils.htmlToExcerpt(normalized, 200));
        note.setContentSize(NoteContentUtils.contentSize(normalized));
        note.setSyncStatus(NoteSyncStatus.SYNCING);
        note.setSyncError(null);
    }

    @Override
    public void syncContentToStorage(Long noteId) {
        if (noteId == null) {
            return;
        }
        if (storageCenterService.isDualStorageEnabled()) {
            syncDualContentToStorage(noteId);
            return;
        }
        syncLegacyContentToStorage(noteId);
    }

    @Override
    public void reconcileAll() {
        if (!storageCenterService.isDualStorageEnabled()) {
            return;
        }
        List<NbNote> notes = nbNoteMapper.selectList(new LambdaQueryWrapper<NbNote>()
                .select(NbNote::getId)
                .eq(NbNote::getDeleted, 0));
        for (NbNote row : notes) {
            if (row.getId() != null) {
                reconcileNote(row.getId());
            }
        }
    }

    @Override
    public void reconcileNote(Long noteId) {
        if (noteId == null) {
            return;
        }
        NbNote note = nbNoteMapper.selectById(noteId);
        if (note == null) {
            return;
        }
        NoteContentRef ref = toRef(note);
        NoteContentReconcileResult result = noteContentVersionService.reconcile(note, ref);
        applyReconcileResult(note, result);

        if (result.isNeedCloudUpload()
                || NoteSyncStatus.CLOUD_PENDING.equals(note.getSyncStatus())
                || NoteSyncStatus.SYNCING.equals(note.getSyncStatus())) {
            syncDualContentToStorage(noteId);
        }
    }

    private void syncDualContentToStorage(Long noteId) {
        for (int round = 0; round < MAX_SYNC_RETRY_ROUNDS; round++) {
            NbNote note = nbNoteMapper.selectById(noteId);
            if (note == null) {
                return;
            }
            String content = readPersistedContent(note);
            String hash = NoteContentUtils.sha256(content);

            if (hash.equals(note.getContentHash()) && NoteSyncStatus.SYNCED.equals(note.getSyncStatus())) {
                return;
            }

            if (hash.equals(note.getContentHash()) && NoteSyncStatus.CLOUD_PENDING.equals(note.getSyncStatus())) {
                retryCloudUploadOnly(note, content);
                return;
            }

            try {
                if (!StringUtils.hasText(note.getStoragePath())) {
                    prepareNewNote(note);
                }
                DualWriteSaveOutcome outcome = dualWriteNoteContentStorage.saveWithOutcome(toRef(note), content);
                applyDualSaveOutcome(note, content, hash, outcome);

                String latest = readPersistedContent(note);
                if (!NoteContentUtils.sha256(latest).equals(hash)) {
                    NbNote pending = nbNoteMapper.selectById(noteId);
                    if (pending != null) {
                        pending.setSyncStatus(NoteSyncStatus.SYNCING);
                        pending.setSyncError(null);
                        nbNoteMapper.updateById(pending);
                    }
                    continue;
                }
                return;
            } catch (Exception e) {
                log.error("后台同步笔记正文失败, noteId={}", noteId, e);
                note.setSyncStatus(NoteSyncStatus.FAILED);
                note.setSyncError(trimError(e.getMessage()));
                nbNoteMapper.updateById(note);
                return;
            }
        }
    }

    private void retryCloudUploadOnly(NbNote note, String content) {
        NoteContentRef ref = toRef(note);
        DualWriteSaveOutcome cloudOutcome = dualWriteNoteContentStorage.saveCloudOnly(ref, content);
        if (cloudOutcome.isCloudSaved()) {
            int version = note.getContentVersion() == null || note.getContentVersion() < 1 ? 1 : note.getContentVersion();
            NoteContentMeta meta = NoteContentMeta.fromContent(content, version);
            noteContentVersionService.writeMetaDual(ref, meta);
            note.setStorageType(DualWriteNoteContentStorage.TYPE);
            note.setStoragePath(cloudOutcome.getCloudPath());
            note.setStorageFsId(cloudOutcome.getCloudFsId());
            note.setSyncStatus(NoteSyncStatus.SYNCED);
            note.setSyncError(null);
            nbNoteMapper.updateById(note);
            log.info("笔记正文已补传至云盘, noteId={}", note.getId());
            return;
        }
        note.setSyncStatus(NoteSyncStatus.CLOUD_PENDING);
        note.setSyncError(cloudOutcome.getCloudError() != null
                ? cloudOutcome.getCloudError()
                : "等待同步至云盘");
        nbNoteMapper.updateById(note);
    }

    private void applyDualSaveOutcome(NbNote note, String content, String hash, DualWriteSaveOutcome outcome) {
        int nextVersion = (note.getContentVersion() == null ? 0 : note.getContentVersion()) + 1;
        NoteContentRef ref = toRef(note);
        NoteContentMeta meta = NoteContentMeta.fromContent(content, nextVersion);
        noteContentVersionService.writeMetaDual(ref, meta);

        note.setStorageType(DualWriteNoteContentStorage.TYPE);
        if (outcome.isCloudSaved()) {
            note.setStoragePath(outcome.getCloudPath());
            note.setStorageFsId(outcome.getCloudFsId());
            note.setSyncStatus(NoteSyncStatus.SYNCED);
            note.setSyncError(null);
        } else {
            note.setStoragePath(outcome.getLocalPath());
            note.setStorageFsId(null);
            note.setSyncStatus(NoteSyncStatus.CLOUD_PENDING);
            note.setSyncError(StringUtils.hasText(outcome.getCloudError())
                    ? outcome.getCloudError()
                    : "等待同步至云盘");
        }
        note.setContentHash(hash);
        note.setContentSize(outcome.getContentSize());
        note.setContentExcerpt(NoteContentUtils.htmlToExcerpt(content, 200));
        note.setContentVersion(nextVersion);
        nbNoteMapper.updateById(note);
    }

    private void applyReconcileResult(NbNote note, NoteContentReconcileResult result) {
        if (result == null) {
            return;
        }
        boolean changed = result.isLocalBackfilled();
        if (result.getMeta() != null) {
            NoteContentMeta meta = result.getMeta();
            if (!meta.getContentHash().equals(note.getContentHash())) {
                note.setContentHash(meta.getContentHash());
                changed = true;
            }
            if (meta.getContentVersion() != (note.getContentVersion() == null ? 0 : note.getContentVersion())) {
                note.setContentVersion(meta.getContentVersion());
                changed = true;
            }
            String content = result.getContent() == null ? "" : result.getContent();
            note.setContentSize(NoteContentUtils.contentSize(content));
            note.setContentExcerpt(NoteContentUtils.htmlToExcerpt(content, 200));
        }
        if (!result.getSyncStatus().equals(note.getSyncStatus())) {
            note.setSyncStatus(result.getSyncStatus());
            changed = true;
        }
        String syncError = result.getSyncError();
        if (syncError != null ? !syncError.equals(note.getSyncError()) : note.getSyncError() != null) {
            note.setSyncError(syncError);
            changed = true;
        }
        if (changed) {
            nbNoteMapper.updateById(note);
        }
    }

    private void syncLegacyContentToStorage(Long noteId) {
        for (int round = 0; round < MAX_SYNC_RETRY_ROUNDS; round++) {
            NbNote note = nbNoteMapper.selectById(noteId);
            if (note == null) {
                return;
            }
            String content = readCachedContent(noteId);
            String hash = NoteContentUtils.sha256(content);
            if (hash.equals(note.getContentHash()) && NoteSyncStatus.SYNCED.equals(note.getSyncStatus())) {
                return;
            }
            try {
                if (!StringUtils.hasText(note.getStoragePath())) {
                    prepareNewNote(note);
                }
                NoteContentStorage storage = resolveWritableStorage(note);
                NoteContentSaveResult result = storage.save(toRef(note), content);
                note.setStorageType(storage.type());
                note.setStoragePath(result.getStoragePath());
                note.setStorageFsId(result.getStorageFsId());
                note.setContentHash(hash);
                note.setContentSize(result.getContentSize());
                note.setContentExcerpt(NoteContentUtils.htmlToExcerpt(content, 200));
                note.setContentVersion((note.getContentVersion() == null ? 0 : note.getContentVersion()) + 1);
                note.setSyncStatus(NoteSyncStatus.SYNCED);
                note.setSyncError(null);
                nbNoteMapper.updateById(note);

                String latest = readCachedContent(noteId);
                if (!NoteContentUtils.sha256(latest).equals(hash)) {
                    NbNote pending = nbNoteMapper.selectById(noteId);
                    if (pending != null) {
                        pending.setSyncStatus(NoteSyncStatus.SYNCING);
                        pending.setSyncError(null);
                        nbNoteMapper.updateById(pending);
                    }
                    continue;
                }
                return;
            } catch (Exception e) {
                log.error("后台同步笔记正文失败, noteId={}", noteId, e);
                note.setSyncStatus(NoteSyncStatus.FAILED);
                note.setSyncError(trimError(e.getMessage()));
                nbNoteMapper.updateById(note);
                return;
            }
        }
    }

    private String readPersistedContent(NbNote note) {
        String cached = readCachedContent(note.getId());
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        if (storageCenterService.isDualStorageEnabled()) {
            return dualWriteNoteContentStorage.loadLocalOnly(toRef(note));
        }
        return resolveStorage(note).load(toRef(note));
    }

    @Override
    public void saveContent(NbNote note, String content) {
        stageContent(note, content);
        syncContentToStorage(note.getId());
        NbNote synced = nbNoteMapper.selectById(note.getId());
        if (synced != null) {
            copyStorageFields(note, synced);
        }
    }

    @Override
    public void deleteContent(NbNote note) {
        if (note == null || note.getId() == null || !StringUtils.hasText(note.getStoragePath())) {
            return;
        }
        try {
            resolveStorage(note).delete(toRef(note));
        } catch (Exception e) {
            log.warn("删除笔记正文失败, noteId={}", note.getId(), e);
        }
        stringRedisTemplate.delete(CACHE_PREFIX + note.getId());
    }

    @Override
    public void prepareNewNote(NbNote note) {
        if (storageCenterService.isDualStorageEnabled()) {
            note.setStorageType(DualWriteNoteContentStorage.TYPE);
            if (baiduPanAuthService.isAuthorized()) {
                note.setStoragePath(baiduPanNoteContentStorage.toStoragePath(note.getId()));
            } else {
                note.setStoragePath(localFileNoteContentStorage.toStoragePath(note.getId()));
            }
        } else {
            NoteContentStorage storage = resolveWritableStorage(note);
            note.setStorageType(storage.type());
            if (storage instanceof BaiduPanNoteContentStorage) {
                note.setStoragePath(baiduPanNoteContentStorage.toStoragePath(note.getId()));
            } else if (storage instanceof LocalFileNoteContentStorage) {
                note.setStoragePath(localFileNoteContentStorage.toStoragePath(note.getId()));
            }
        }
        note.setContentHash("");
        note.setContentSize(0L);
        note.setContentVersion(0);
        note.setContentExcerpt("");
        note.setSyncStatus(NoteSyncStatus.SYNCED);
    }

    private String readCachedContent(Long noteId) {
        String cached = stringRedisTemplate.opsForValue().get(CACHE_PREFIX + noteId);
        return cached == null ? "" : cached;
    }

    private void copyStorageFields(NbNote target, NbNote source) {
        target.setStorageType(source.getStorageType());
        target.setStoragePath(source.getStoragePath());
        target.setStorageFsId(source.getStorageFsId());
        target.setContentHash(source.getContentHash());
        target.setContentSize(source.getContentSize());
        target.setContentVersion(source.getContentVersion());
        target.setContentExcerpt(source.getContentExcerpt());
        target.setSyncStatus(source.getSyncStatus());
        target.setSyncError(source.getSyncError());
    }

    private NoteContentStorage resolveWritableStorage(NbNote note) {
        if (storageCenterService.isDualStorageEnabled()) {
            dualWriteNoteContentStorage.ensureRoot();
            return dualWriteNoteContentStorage;
        }
        if (useBaiduPan()) {
            try {
                baiduPanAuthService.requireAccessToken();
                baiduPanNoteContentStorage.ensureRoot();
                return baiduPanNoteContentStorage;
            } catch (BusinessException ex) {
                if ("BAIDU_PAN".equalsIgnoreCase(noteStorageProperties.getType())) {
                    throw ex;
                }
            }
        }
        return localFileNoteContentStorage;
    }

    private NoteContentStorage resolveStorage(NbNote note) {
        if (storageCenterService.isDualStorageEnabled()) {
            return dualWriteNoteContentStorage;
        }
        String storageType = note.getStorageType();
        if (StringUtils.hasText(storageType) && storageMap.containsKey(storageType)) {
            return storageMap.get(storageType);
        }
        if (useBaiduPan() && baiduPanAuthService.isAuthorized()) {
            return baiduPanNoteContentStorage;
        }
        return localFileNoteContentStorage;
    }

    private boolean useBaiduPan() {
        return "BAIDU_PAN".equalsIgnoreCase(noteStorageProperties.getType());
    }

    private NoteContentRef toRef(NbNote note) {
        return NoteContentRef.builder()
                .noteId(note.getId())
                .storageType(note.getStorageType())
                .storagePath(note.getStoragePath())
                .storageFsId(note.getStorageFsId())
                .build();
    }

    private void cacheContent(Long noteId, String content) {
        ensureCacheCapacity(content);
        stringRedisTemplate.opsForValue().set(
                CACHE_PREFIX + noteId,
                content == null ? "" : content,
                Duration.ofSeconds(resolveCacheTtlSeconds())
        );
    }

    private void ensureCacheCapacity(String content) {
        long incoming = content == null ? 0L : content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        try {
            storageCenterService.enforceCacheLimit(incoming);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Redis 缓存容量检查失败: {}", ex.getMessage());
        }
    }

    private long resolveCacheTtlSeconds() {
        Long ttl = storageCenterService.getConfig().getCacheTtlSeconds();
        if (ttl != null && ttl > 0) {
            return ttl;
        }
        return noteStorageProperties.getCacheTtlSeconds();
    }

    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "unknown";
        }
        return message.length() > 480 ? message.substring(0, 480) : message;
    }
}
