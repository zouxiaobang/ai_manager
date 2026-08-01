package com.ai.manager.system.service.storage;

import com.ai.manager.system.domain.storage.ChainSaveOutcome;
import com.ai.manager.system.domain.storage.NoteContentRef;
import com.ai.manager.system.domain.storage.NoteContentSaveResult;
import com.ai.manager.system.util.NoteContentUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 有序存储链 — 按优先级排列的存储层列表。
 *
 * <p>行为：</p>
 * <ul>
 *   <li><b>save</b>：先写入第 0 层（主存储），再依次尝试写入后续层（远程存储），
 *       任意一个远程层成功后即返回双写结果</li>
 *   <li><b>load</b>：按存储层顺序依次尝试读取，返回第一个有效的非错误内容</li>
 *   <li><b>delete</b>：遍历所有层执行删除</li>
 * </ul>
 *
 * <p>关键约定：各存储层使用自己独立的路径方案（{@code toStoragePath(noteId)}），
 * 因此传入的 {@link NoteContentRef} 中的 {@code storagePath} 会被忽略，
 * 仅保留 {@code noteId} 用于构建各存储层自身的路径。</p>
 */
@Slf4j
public class OrderedChain implements StorageChain {

    /** 链标识名（多于一层时使用） */
    public static final String CHAIN_NAME = "CHAIN";

    /** 所有存储层，按优先级排列 */
    private final List<NoteContentStorage> storages;

    /** 主存储（第 0 层） */
    private final NoteContentStorage primary;

    /** 远程存储（第 1..N 层） */
    private final List<NoteContentStorage> remotes;

    /**
     * @param storages 按优先级排列的存储层列表，至少包含一个元素
     */
    public OrderedChain(List<NoteContentStorage> storages) {
        if (storages == null || storages.isEmpty()) {
            throw new IllegalArgumentException("storages must not be empty");
        }
        this.storages = List.copyOf(storages);
        this.primary = storages.get(0);
        this.remotes = storages.size() > 1
                ? List.copyOf(storages.subList(1, storages.size()))
                : List.of();
    }

    @Override
    public String name() {
        if (storages.size() == 1) {
            return storages.get(0).type();
        }
        return CHAIN_NAME;
    }

    @Override
    public ChainSaveOutcome save(NoteContentRef ref, String content) {
        // 为每层存储重建 ref（只保留 noteId，各存储自行决定路径）
        NoteContentSaveResult primaryResult = saveOrThrow(primary, refOnly(ref), content);

        if (remotes.isEmpty()) {
            return ChainSaveOutcome.primaryOnly(
                    primaryResult.getStoragePath(), primaryResult.getContentSize());
        }

        // 尝试写入各远程存储，任意一个成功即返回
        for (NoteContentStorage remote : remotes) {
            try {
                NoteContentSaveResult remoteResult = remote.save(refOnly(ref), content);
                return ChainSaveOutcome.dual(
                        primaryResult.getStoragePath(),
                        remoteResult.getStoragePath(),
                        remoteResult.getStorageFsId(),
                        primaryResult.getContentSize()
                );
            } catch (Exception ex) {
                log.warn("同步到远程存储 {} 失败, noteId={}: {}",
                        remote.type(), ref.getNoteId(), ex.getMessage());
            }
        }

        // 所有远程都失败，仅主存储成功
        return ChainSaveOutcome.primaryOnly(
                primaryResult.getStoragePath(), primaryResult.getContentSize());
    }

    @Override
    public String load(NoteContentRef ref) {
        for (NoteContentStorage storage : storages) {
            try {
                String content = storage.load(refOnly(ref));
                if (StringUtils.hasText(content) && !isErrorBody(content)) {
                    return content;
                }
            } catch (Exception e) {
                log.debug("从存储层 {} 读取失败, noteId={}: {}",
                        storage.type(), ref.getNoteId(), e.getMessage());
            }
        }
        return "";
    }

    @Override
    public ChainSaveOutcome saveRemoteOnly(NoteContentRef ref, String content) {
        for (NoteContentStorage remote : remotes) {
            try {
                NoteContentSaveResult result = remote.save(refOnly(ref), content);
                return ChainSaveOutcome.remoteOnly(
                        result.getStoragePath(), result.getStorageFsId(), result.getContentSize());
            } catch (Exception ex) {
                log.warn("补传到远程存储 {} 失败, noteId={}: {}",
                        remote.type(), ref.getNoteId(), ex.getMessage());
            }
        }
        return ChainSaveOutcome.failed(content == null ? 0 : content.getBytes().length,
                "所有远程存储均写入失败");
    }

    @Override
    public String loadPrimaryOnly(NoteContentRef ref) {
        try {
            String content = primary.load(refOnly(ref));
            return StringUtils.hasText(content) ? content : "";
        } catch (Exception e) {
            log.debug("从主存储 {} 读取失败, noteId={}: {}",
                    primary.type(), ref.getNoteId(), e.getMessage());
            return "";
        }
    }

    @Override
    public String loadRemoteOnly(NoteContentRef ref) {
        for (NoteContentStorage remote : remotes) {
            try {
                String content = remote.load(refOnly(ref));
                if (StringUtils.hasText(content) && !isErrorBody(content)) {
                    return content;
                }
            } catch (Exception e) {
                log.debug("从远程存储 {} 读取失败, noteId={}: {}",
                        remote.type(), ref.getNoteId(), e.getMessage());
            }
        }
        return "";
    }

    @Override
    public boolean hasRemoteContent(NoteContentRef ref) {
        return StringUtils.hasText(loadRemoteOnly(ref));
    }

    @Override
    public boolean reconcilePrimaryFromRemote(NoteContentRef ref) {
        if (StringUtils.hasText(loadPrimaryOnly(ref))) {
            return false;
        }
        String remoteContent = loadRemoteOnly(ref);
        if (!StringUtils.hasText(remoteContent)) {
            return false;
        }
        try {
            primary.save(refOnly(ref), remoteContent);
            log.info("已从远程存储回填到主存储, noteId={}", ref.getNoteId());
            return true;
        } catch (Exception ex) {
            log.warn("远程存储回填主存储失败, noteId={}: {}",
                    ref.getNoteId(), ex.getMessage());
            return false;
        }
    }

    @Override
    public void delete(NoteContentRef ref) {
        for (NoteContentStorage storage : storages) {
            try {
                storage.delete(refOnly(ref));
            } catch (Exception ex) {
                log.warn("从存储层 {} 删除失败, noteId={}: {}",
                        storage.type(), ref.getNoteId(), ex.getMessage());
            }
        }
    }

    @Override
    public void ensureRoot() {
        storages.forEach(NoteContentStorage::ensureRoot);
    }

    // ===== 内部工具 =====

    /**
     * 为存储层重建 ref，只保留 noteId 和 storageFsId。
     * <p>各 {@link NoteContentStorage} 有自己的路径方案（{@code toStoragePath(noteId)}），
     * 不应使用来自其他存储层的 {@code storagePath}，否则会导致路径错乱。
     * 但 {@code storageFsId} 是文件系统级别的标识，与路径方案无关，应当保留以备云端读取加速。</p>
     */
    private static NoteContentRef refOnly(NoteContentRef original) {
        return NoteContentRef.builder()
                .noteId(original.getNoteId())
                .storageFsId(original.getStorageFsId())
                .build();
    }

    private static NoteContentSaveResult saveOrThrow(
            NoteContentStorage storage, NoteContentRef ref, String content) {
        try {
            return storage.save(ref, content);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "保存到主存储 " + storage.type() + " 失败, noteId=" + ref.getNoteId(), ex);
        }
    }

    private static boolean isErrorBody(String content) {
        return NoteContentUtils.isBaiduApiErrorBody(content);
    }
}
