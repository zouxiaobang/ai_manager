package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.storage.NoteContentRef;
import com.ai.manager.system.domain.storage.NoteContentSaveResult;
import com.ai.manager.system.mapper.NbNoteMapper;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.NbNoteContentService;
import com.ai.manager.system.service.storage.BaiduPanNoteContentStorage;
import com.ai.manager.system.service.storage.LocalFileNoteContentStorage;
import com.ai.manager.system.service.storage.NoteContentStorage;
import com.ai.manager.system.util.NoteContentUtils;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final NbNoteMapper nbNoteMapper;

    private Map<String, NoteContentStorage> storageMap;

    @jakarta.annotation.PostConstruct
    void init() {
        List<NoteContentStorage> storages = List.of(baiduPanNoteContentStorage, localFileNoteContentStorage);
        storageMap = storages.stream().collect(Collectors.toMap(NoteContentStorage::type, Function.identity()));
        localFileNoteContentStorage.ensureRoot();
    }

    @Override
    public String loadContent(NbNote note) {
        if (note == null || note.getId() == null) {
            return "";
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
        NoteContentStorage storage = resolveStorage(note.getStorageType());
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
            if (pendingHash.equals(note.getContentHash()) && "SYNCED".equals(note.getSyncStatus())) {
                return;
            }
        }
        cacheContent(note.getId(), normalized);
        note.setContentExcerpt(NoteContentUtils.htmlToExcerpt(normalized, 200));
        note.setContentSize(NoteContentUtils.contentSize(normalized));
        note.setSyncStatus("SYNCING");
        note.setSyncError(null);
    }

    @Override
    public void syncContentToStorage(Long noteId) {
        if (noteId == null) {
            return;
        }
        for (int round = 0; round < MAX_SYNC_RETRY_ROUNDS; round++) {
            NbNote note = nbNoteMapper.selectById(noteId);
            if (note == null) {
                return;
            }
            String content = readCachedContent(noteId);
            String hash = NoteContentUtils.sha256(content);
            if (hash.equals(note.getContentHash()) && "SYNCED".equals(note.getSyncStatus())) {
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
                note.setSyncStatus("SYNCED");
                note.setSyncError(null);
                nbNoteMapper.updateById(note);

                String latest = readCachedContent(noteId);
                if (!NoteContentUtils.sha256(latest).equals(hash)) {
                    NbNote pending = nbNoteMapper.selectById(noteId);
                    if (pending != null) {
                        pending.setSyncStatus("SYNCING");
                        pending.setSyncError(null);
                        nbNoteMapper.updateById(pending);
                    }
                    continue;
                }
                return;
            } catch (Exception e) {
                log.error("后台同步笔记正文失败, noteId={}", noteId, e);
                note.setSyncStatus("FAILED");
                note.setSyncError(trimError(e.getMessage()));
                nbNoteMapper.updateById(note);
                return;
            }
        }
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
            resolveStorage(note.getStorageType()).delete(toRef(note));
        } catch (Exception e) {
            log.warn("删除笔记正文失败, noteId={}", note.getId(), e);
        }
        stringRedisTemplate.delete(CACHE_PREFIX + note.getId());
    }

    @Override
    public void prepareNewNote(NbNote note) {
        NoteContentStorage storage = resolveWritableStorage(note);
        note.setStorageType(storage.type());
        if (storage instanceof BaiduPanNoteContentStorage baiduStorage) {
            note.setStoragePath(baiduStorage.toStoragePath(note.getId()));
        } else if (storage instanceof LocalFileNoteContentStorage localStorage) {
            note.setStoragePath(localStorage.toStoragePath(note.getId()));
        }
        note.setContentHash("");
        note.setContentSize(0L);
        note.setContentVersion(0);
        note.setContentExcerpt("");
        note.setSyncStatus("SYNCED");
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

    private NoteContentStorage resolveStorage(String storageType) {
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
        stringRedisTemplate.opsForValue().set(
                CACHE_PREFIX + noteId,
                content == null ? "" : content,
                Duration.ofSeconds(noteStorageProperties.getCacheTtlSeconds())
        );
    }

    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "unknown";
        }
        return message.length() > 480 ? message.substring(0, 480) : message;
    }
}
