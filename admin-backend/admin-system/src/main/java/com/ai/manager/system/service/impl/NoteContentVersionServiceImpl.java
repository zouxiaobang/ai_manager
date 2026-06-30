package com.ai.manager.system.service.impl;

import com.ai.manager.system.client.BaiduPanClient;
import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.storage.NoteContentMeta;
import com.ai.manager.system.domain.storage.NoteContentReconcileResult;
import com.ai.manager.system.domain.storage.NoteContentRef;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.NoteContentVersionService;
import com.ai.manager.system.service.StorageCenterService;
import com.ai.manager.system.service.storage.BaiduPanNoteContentStorage;
import com.ai.manager.system.service.storage.DualWriteNoteContentStorage;
import com.ai.manager.system.service.storage.LocalFileNoteContentStorage;
import com.ai.manager.system.service.support.NoteSyncStatus;
import com.ai.manager.system.util.NoteContentUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteContentVersionServiceImpl implements NoteContentVersionService {

    private static final String META_SUFFIX = ".meta.json";

    private final DualWriteNoteContentStorage dualWriteNoteContentStorage;
    private final LocalFileNoteContentStorage localFileNoteContentStorage;
    private final BaiduPanNoteContentStorage baiduPanNoteContentStorage;
    private final BaiduPanAuthService baiduPanAuthService;
    private final StorageCenterService storageCenterService;
    private final NoteStorageProperties noteStorageProperties;
    private final BaiduPanProperties baiduPanProperties;
    private final BaiduPanClient baiduPanClient;
    private final ObjectMapper objectMapper;

    @Override
    public NoteContentReconcileResult reconcile(NbNote note, NoteContentRef ref) {
        ContentSide local = loadLocalSide(ref, note);
        ContentSide cloud = loadCloudSide(ref, note, local);

        if (!local.hasContent() && !cloud.hasContent()) {
            return NoteContentReconcileResult.builder()
                    .content("")
                    .meta(NoteContentMeta.fromContent("", fallbackVersion(note)))
                    .source("LOCAL")
                    .syncStatus(NoteSyncStatus.SYNCED)
                    .needCloudUpload(false)
                    .localBackfilled(false)
                    .build();
        }

        if (!local.hasContent() && cloud.hasContent()) {
            backfillLocal(ref, cloud);
            return buildResult(cloud, "CLOUD", NoteSyncStatus.SYNCED, false, true);
        }

        if (local.hasContent() && !cloud.hasContent()) {
            boolean needUpload = storageCenterService.isDualStorageEnabled()
                    && baiduPanAuthService.isAuthorized();
            return buildResult(local, "LOCAL", needUpload ? NoteSyncStatus.CLOUD_PENDING : NoteSyncStatus.SYNCED,
                    needUpload, false);
        }

        boolean sameHash = local.resolvedMeta().getContentHash().equals(cloud.resolvedMeta().getContentHash());
        if (sameHash) {
            return buildResult(local, "LOCAL", NoteSyncStatus.SYNCED, false, false);
        }

        ContentSide winner = pickWinner(local, cloud);
        if (winner == cloud) {
            backfillLocal(ref, cloud);
            log.info("笔记正文以云盘版本为准 noteId={} localV={} cloudV={}",
                    ref.getNoteId(), local.resolvedMeta().getContentVersion(), cloud.resolvedMeta().getContentVersion());
            return buildResult(cloud, "CLOUD", NoteSyncStatus.SYNCED, false, true);
        }

        log.info("笔记正文以本地版本为准 noteId={} localV={} cloudV={}",
                ref.getNoteId(), local.resolvedMeta().getContentVersion(), cloud.resolvedMeta().getContentVersion());
        return buildResult(local, "LOCAL", NoteSyncStatus.CLOUD_PENDING, true, false);
    }

    @Override
    public void writeMetaDual(NoteContentRef ref, NoteContentMeta meta) {
        writeLocalMeta(ref, meta);
        if (storageCenterService.isDualStorageEnabled() && baiduPanAuthService.isAuthorized()) {
            writeCloudMeta(ref, meta);
        }
    }

    public void deleteMeta(NoteContentRef ref) {
        try {
            Files.deleteIfExists(resolveLocalMetaPath(ref));
        } catch (IOException ex) {
            log.warn("删除本地笔记元数据失败 noteId={}: {}", ref.getNoteId(), ex.getMessage());
        }
        if (baiduPanAuthService.isAuthorized()) {
            try {
                String accessToken = baiduPanAuthService.requireAccessToken();
                baiduPanClient.delete(accessToken, cloudMetaPath(ref));
            } catch (Exception ex) {
                log.debug("删除云盘笔记元数据失败 noteId={}: {}", ref.getNoteId(), ex.getMessage());
            }
        }
    }

    private ContentSide pickWinner(ContentSide local, ContentSide cloud) {
        int localVersion = local.resolvedMeta().getContentVersion();
        int cloudVersion = cloud.resolvedMeta().getContentVersion();
        if (localVersion != cloudVersion) {
            return cloudVersion > localVersion ? cloud : local;
        }
        return cloud.resolvedMeta().updatedAtInstant().isAfter(local.resolvedMeta().updatedAtInstant()) ? cloud : local;
    }

    private void backfillLocal(NoteContentRef ref, ContentSide cloud) {
        localFileNoteContentStorage.save(toLocalRef(ref), cloud.content());
        writeLocalMeta(ref, cloud.resolvedMeta());
        storageCenterService.onFileWritten(
                StorageCenterServiceImpl.ZONE_NOTEBOOK_CONTENT,
                cloud.content().getBytes(StandardCharsets.UTF_8).length
        );
    }

    private ContentSide loadLocalSide(NoteContentRef ref, NbNote note) {
        String content = dualWriteNoteContentStorage.loadLocalOnly(ref);
        if (!StringUtils.hasText(content)) {
            return ContentSide.empty();
        }
        NoteContentMeta meta = readLocalMeta(ref);
        if (meta == null) {
            meta = deriveMeta(content, note, null, localFileUpdatedAt(ref));
        }
        return new ContentSide(content, meta);
    }

    private ContentSide loadCloudSide(NoteContentRef ref, NbNote note, ContentSide local) {
        if (!storageCenterService.isDualStorageEnabled() || !baiduPanAuthService.isAuthorized()) {
            return ContentSide.empty();
        }
        String content = dualWriteNoteContentStorage.loadCloudOnly(ref);
        if (!StringUtils.hasText(content) || NoteContentUtils.isBaiduApiErrorBody(content)) {
            return ContentSide.empty();
        }
        NoteContentMeta meta = readCloudMeta(ref);
        if (meta == null) {
            String otherHash = local.hasContent() ? local.resolvedMeta().getContentHash() : null;
            meta = deriveMeta(content, note, otherHash, Instant.EPOCH);
        }
        return new ContentSide(content, meta);
    }

    private NoteContentMeta deriveMeta(String content, NbNote note, String otherHash, Instant updatedAt) {
        String hash = NoteContentUtils.sha256(content);
        int base = fallbackVersion(note);
        int version = base;
        String dbHash = note.getContentHash();
        if (StringUtils.hasText(dbHash)) {
            if (hash.equals(dbHash)) {
                version = base;
            } else if (StringUtils.hasText(otherHash) && hash.equals(otherHash)) {
                version = base;
            } else {
                version = base + 1;
            }
        }
        return NoteContentMeta.builder()
                .contentHash(hash)
                .contentVersion(Math.max(version, 1))
                .updatedAt(updatedAt.toString())
                .build();
    }

    private NoteContentReconcileResult buildResult(
            ContentSide side,
            String source,
            String syncStatus,
            boolean needCloudUpload,
            boolean localBackfilled
    ) {
        return NoteContentReconcileResult.builder()
                .content(side.content())
                .meta(side.resolvedMeta())
                .source(source)
                .syncStatus(syncStatus)
                .syncError(needCloudUpload ? "等待同步至云盘" : null)
                .needCloudUpload(needCloudUpload)
                .localBackfilled(localBackfilled)
                .build();
    }

    private int fallbackVersion(NbNote note) {
        return note.getContentVersion() == null || note.getContentVersion() < 1 ? 1 : note.getContentVersion();
    }

    private Instant localFileUpdatedAt(NoteContentRef ref) {
        Path file = resolveLocalHtmlPath(ref);
        try {
            if (Files.isRegularFile(file)) {
                return Files.getLastModifiedTime(file).toInstant();
            }
        } catch (IOException ignored) {
            // use now
        }
        return Instant.now();
    }

    private NoteContentMeta readLocalMeta(NoteContentRef ref) {
        Path metaPath = resolveLocalMetaPath(ref);
        if (!Files.isRegularFile(metaPath)) {
            return null;
        }
        try {
            return objectMapper.readValue(metaPath.toFile(), NoteContentMeta.class);
        } catch (IOException ex) {
            log.debug("读取本地笔记元数据失败 noteId={}: {}", ref.getNoteId(), ex.getMessage());
            return null;
        }
    }

    private void writeLocalMeta(NoteContentRef ref, NoteContentMeta meta) {
        try {
            Path metaPath = resolveLocalMetaPath(ref);
            Files.createDirectories(metaPath.getParent());
            objectMapper.writeValue(metaPath.toFile(), meta);
        } catch (IOException ex) {
            log.warn("写入本地笔记元数据失败 noteId={}: {}", ref.getNoteId(), ex.getMessage());
        }
    }

    private NoteContentMeta readCloudMeta(NoteContentRef ref) {
        try {
            String accessToken = baiduPanAuthService.requireAccessToken();
            String json = baiduPanClient.download(accessToken, cloudMetaPath(ref), null);
            if (!StringUtils.hasText(json) || NoteContentUtils.isBaiduApiErrorBody(json)) {
                return null;
            }
            return objectMapper.readValue(json, NoteContentMeta.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void writeCloudMeta(NoteContentRef ref, NoteContentMeta meta) {
        try {
            baiduPanNoteContentStorage.ensureRoot();
            String accessToken = baiduPanAuthService.requireAccessToken();
            baiduPanClient.upload(accessToken, cloudMetaPath(ref), objectMapper.writeValueAsBytes(meta));
        } catch (Exception ex) {
            log.warn("写入云盘笔记元数据失败 noteId={}: {}", ref.getNoteId(), ex.getMessage());
        }
    }

    private Path resolveLocalHtmlPath(NoteContentRef ref) {
        return Paths.get(noteStorageProperties.getLocalRoot())
                .resolve(localFileNoteContentStorage.toStoragePath(ref.getNoteId()))
                .normalize();
    }

    private Path resolveLocalMetaPath(NoteContentRef ref) {
        return Paths.get(noteStorageProperties.getLocalRoot())
                .resolve("notes/" + ref.getNoteId() + META_SUFFIX)
                .normalize();
    }

    private String cloudMetaPath(NoteContentRef ref) {
        return baiduPanProperties.notesDir() + "/" + ref.getNoteId() + META_SUFFIX;
    }

    private NoteContentRef toLocalRef(NoteContentRef ref) {
        return NoteContentRef.builder()
                .noteId(ref.getNoteId())
                .storageType(LocalFileNoteContentStorage.TYPE)
                .storagePath(localFileNoteContentStorage.toStoragePath(ref.getNoteId()))
                .storageFsId(null)
                .build();
    }

    private static final class ContentSide {
        private final String content;
        private final NoteContentMeta meta;

        private ContentSide(String content, NoteContentMeta meta) {
            this.content = content;
            this.meta = meta;
        }

        static ContentSide empty() {
            return new ContentSide("", null);
        }

        String content() {
            return content;
        }

        boolean hasContent() {
            return StringUtils.hasText(content);
        }

        NoteContentMeta resolvedMeta() {
            if (meta != null) {
                return meta;
            }
            return NoteContentMeta.fromContent(content == null ? "" : content, 1);
        }
    }
}
