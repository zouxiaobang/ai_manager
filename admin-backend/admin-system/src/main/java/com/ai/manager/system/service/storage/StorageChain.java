package com.ai.manager.system.service.storage;

import com.ai.manager.system.domain.storage.ChainSaveOutcome;
import com.ai.manager.system.domain.storage.NoteContentRef;

/**
 * 存储链 — 一组有序的 {@link NoteContentStorage} 层。
 * <p>职责：</p>
 * <ul>
 *   <li>保存时写入主存储（第 0 层），尝试同步到远程存储（第 1..N 层）</li>
 *   <li>读取时依次尝试各层，直到读到有效内容</li>
 *   <li>删除时遍历所有层</li>
 * </ul>
 * <p>设计意图：将存储编排逻辑从业务服务中剥离，使 {@code NbNoteContentServiceImpl}
 * 只需面向 {@code StorageChain} 编程，无需关心具体存储类型。</p>
 */
public interface StorageChain {

    /** 链标识名 */
    String name();

    /** 保存到所有存储层 */
    ChainSaveOutcome save(NoteContentRef ref, String content);

    /** 从存储链读取（依次尝试各层，直到命中有效内容） */
    String load(NoteContentRef ref);

    /** 仅写入远程存储层（用于 CLOUD_PENDING 重试） */
    ChainSaveOutcome saveRemoteOnly(NoteContentRef ref, String content);

    /** 仅从主存储（第 0 层）读取 */
    String loadPrimaryOnly(NoteContentRef ref);

    /** 仅从远程存储层读取 */
    String loadRemoteOnly(NoteContentRef ref);

    /** 远程存储层是否有内容 */
    boolean hasRemoteContent(NoteContentRef ref);

    /** 从远程存储层回填到主存储层 */
    boolean reconcilePrimaryFromRemote(NoteContentRef ref);

    void delete(NoteContentRef ref);

    void ensureRoot();
}
