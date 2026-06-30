package com.ai.manager.system.service.storage;

import com.ai.manager.system.client.BaiduPanClient;
import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.storage.DualWriteSaveOutcome;
import com.ai.manager.system.domain.storage.NoteContentRef;
import com.ai.manager.system.domain.storage.NoteContentSaveResult;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.StorageCenterService;
import com.ai.manager.system.service.impl.StorageCenterServiceImpl;
import com.ai.manager.system.service.support.StoragePathSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class DualWriteNoteContentStorage implements NoteContentStorage {

    public static final String TYPE = "DUAL";

    private final LocalFileNoteContentStorage localFileNoteContentStorage;
    private final BaiduPanNoteContentStorage baiduPanNoteContentStorage;
    private final BaiduPanAuthService baiduPanAuthService;
    private final StorageCenterService storageCenterService;
    private final NoteStorageProperties noteStorageProperties;
    private final BaiduPanProperties baiduPanProperties;
    private final BaiduPanClient baiduPanClient;

    private static final String META_SUFFIX = ".meta.json";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public NoteContentSaveResult save(NoteContentRef ref, String content) {
        DualWriteSaveOutcome outcome = saveWithOutcome(ref, content);
        if (outcome.isCloudSaved()) {
            return NoteContentSaveResult.builder()
                    .storagePath(outcome.getCloudPath())
                    .storageFsId(outcome.getCloudFsId())
                    .contentSize(outcome.getContentSize())
                    .build();
        }
        return NoteContentSaveResult.builder()
                .storagePath(outcome.getLocalPath())
                .storageFsId(null)
                .contentSize(outcome.getContentSize())
                .build();
    }

    public DualWriteSaveOutcome saveWithOutcome(NoteContentRef ref, String content) {
        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        storageCenterService.assertWritable(StorageCenterServiceImpl.ZONE_NOTEBOOK_CONTENT, bytes.length);

        NoteContentRef localRef = toLocalRef(ref);
        NoteContentSaveResult localResult;
        try {
            localResult = localFileNoteContentStorage.save(localRef, content);
            storageCenterService.onFileWritten(StorageCenterServiceImpl.ZONE_NOTEBOOK_CONTENT, localResult.getContentSize());
        } catch (Exception ex) {
            throw new IllegalStateException("保存笔记正文到本地失败, noteId=" + ref.getNoteId(), ex);
        }

        if (!shouldSyncToCloud()) {
            return DualWriteSaveOutcome.localOnly(
                    localResult.getStoragePath(),
                    localResult.getContentSize(),
                    "双写已关闭或未连接百度网盘"
            );
        }

        DualWriteSaveOutcome cloudOutcome = saveCloudOnly(ref, content);
        if (cloudOutcome.isCloudSaved()) {
            return DualWriteSaveOutcome.dual(
                    localResult.getStoragePath(),
                    cloudOutcome.getCloudPath(),
                    cloudOutcome.getCloudFsId(),
                    localResult.getContentSize()
            );
        }
        return DualWriteSaveOutcome.localOnly(
                localResult.getStoragePath(),
                localResult.getContentSize(),
                cloudOutcome.getCloudError()
        );
    }

    public DualWriteSaveOutcome saveCloudOnly(NoteContentRef ref, String content) {
        if (!shouldSyncToCloud()) {
            return DualWriteSaveOutcome.builder()
                    .localSaved(false)
                    .cloudSaved(false)
                    .cloudError("未连接百度网盘")
                    .build();
        }
        try {
            NoteContentSaveResult cloudResult = baiduPanNoteContentStorage.save(toCloudRef(ref), content);
            return DualWriteSaveOutcome.builder()
                    .localSaved(false)
                    .cloudSaved(true)
                    .cloudPath(cloudResult.getStoragePath())
                    .cloudFsId(cloudResult.getStorageFsId())
                    .contentSize(cloudResult.getContentSize())
                    .build();
        } catch (Exception ex) {
            log.warn("笔记正文上传云盘失败, noteId={}: {}", ref.getNoteId(), ex.getMessage());
            return DualWriteSaveOutcome.builder()
                    .localSaved(false)
                    .cloudSaved(false)
                    .cloudError(trimError(ex.getMessage()))
                    .build();
        }
    }

    @Override
    public String load(NoteContentRef ref) {
        reconcileLocalFromCloud(ref);
        String local = loadLocalOnly(ref);
        if (StringUtils.hasText(local)) {
            return local;
        }
        return loadCloudOnly(ref);
    }

    public String loadLocalOnly(NoteContentRef ref) {
        try {
            String content = localFileNoteContentStorage.load(toLocalRef(ref));
            return StringUtils.hasText(content) ? content : "";
        } catch (Exception ex) {
            log.debug("读取本地笔记正文失败, noteId={}: {}", ref.getNoteId(), ex.getMessage());
            return "";
        }
    }

    public String loadCloudOnly(NoteContentRef ref) {
        if (!baiduPanAuthService.isAuthorized()) {
            return "";
        }
        try {
            return baiduPanNoteContentStorage.load(toCloudRef(ref));
        } catch (Exception ex) {
            log.warn("从百度网盘读取笔记正文失败, noteId={}: {}", ref.getNoteId(), ex.getMessage());
            return "";
        }
    }

    public boolean hasLocalContent(NoteContentRef ref) {
        Path file = resolveLocalPath(ref);
        try {
            return Files.isRegularFile(file) && Files.size(file) > 0;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 云盘有、本地无时，将正文回填到本地。
     *
     * @return 是否发生了回填
     */
    public boolean reconcileLocalFromCloud(NoteContentRef ref) {
        if (!storageCenterService.isDualStorageEnabled() || hasLocalContent(ref)) {
            return false;
        }
        String cloud = loadCloudOnly(ref);
        if (!StringUtils.hasText(cloud)) {
            return false;
        }
        try {
            localFileNoteContentStorage.save(toLocalRef(ref), cloud);
            storageCenterService.onFileWritten(
                    StorageCenterServiceImpl.ZONE_NOTEBOOK_CONTENT,
                    cloud.getBytes(StandardCharsets.UTF_8).length
            );
            log.info("笔记正文已从云盘回填本地, noteId={}", ref.getNoteId());
            return true;
        } catch (Exception ex) {
            log.warn("云盘正文回填本地失败, noteId={}: {}", ref.getNoteId(), ex.getMessage());
            return false;
        }
    }

    @Override
    public void delete(NoteContentRef ref) {
        try {
            localFileNoteContentStorage.delete(toLocalRef(ref));
            deleteLocalMeta(ref);
        } catch (Exception ex) {
            log.warn("删除本地笔记正文失败, noteId={}: {}", ref.getNoteId(), ex.getMessage());
        }
        if (baiduPanAuthService.isAuthorized()) {
            try {
                baiduPanNoteContentStorage.delete(toCloudRef(ref));
                deleteCloudMeta(ref);
            } catch (Exception ex) {
                log.warn("删除云盘笔记正文失败, noteId={}: {}", ref.getNoteId(), ex.getMessage());
            }
        }
    }

    @Override
    public void ensureRoot() {
        localFileNoteContentStorage.ensureRoot();
        if (baiduPanAuthService.isAuthorized()) {
            baiduPanNoteContentStorage.ensureRoot();
        }
    }

    private boolean shouldSyncToCloud() {
        return storageCenterService.isDualStorageEnabled() && baiduPanAuthService.isAuthorized();
    }

    private Path resolveLocalPath(NoteContentRef ref) {
        String relative = localFileNoteContentStorage.toStoragePath(ref.getNoteId());
        return StoragePathSupport.resolveUploadBasePath(noteStorageProperties.getLocalRoot())
                .resolve(relative)
                .normalize();
    }

    private NoteContentRef toLocalRef(NoteContentRef ref) {
        return NoteContentRef.builder()
                .noteId(ref.getNoteId())
                .storageType(LocalFileNoteContentStorage.TYPE)
                .storagePath(localFileNoteContentStorage.toStoragePath(ref.getNoteId()))
                .storageFsId(null)
                .build();
    }

    private NoteContentRef toCloudRef(NoteContentRef ref) {
        String cloudPath = StringUtils.hasText(ref.getStoragePath())
                && ref.getStoragePath().startsWith("/apps/")
                ? ref.getStoragePath()
                : baiduPanNoteContentStorage.toStoragePath(ref.getNoteId());
        return NoteContentRef.builder()
                .noteId(ref.getNoteId())
                .storageType(BaiduPanNoteContentStorage.TYPE)
                .storagePath(cloudPath)
                .storageFsId(ref.getStorageFsId())
                .build();
    }

    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "云盘同步失败";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    private void deleteLocalMeta(NoteContentRef ref) {
        try {
            Path meta = Paths.get(noteStorageProperties.getLocalRoot())
                    .resolve("notes/" + ref.getNoteId() + META_SUFFIX)
                    .normalize();
            Files.deleteIfExists(meta);
        } catch (IOException ex) {
            log.debug("删除本地笔记元数据失败, noteId={}: {}", ref.getNoteId(), ex.getMessage());
        }
    }

    private void deleteCloudMeta(NoteContentRef ref) {
        try {
            String accessToken = baiduPanAuthService.requireAccessToken();
            baiduPanClient.delete(accessToken, baiduPanProperties.notesDir() + "/" + ref.getNoteId() + META_SUFFIX);
        } catch (Exception ex) {
            log.debug("删除云盘笔记元数据失败, noteId={}: {}", ref.getNoteId(), ex.getMessage());
        }
    }
}
