package com.ai.manager.system.domain.storage;

import lombok.Builder;
import lombok.Value;

/**
 * 存储链保存结果。
 * 替代 DualWriteSaveOutcome，支持 N 层存储链的保存结果表达：
 * - 主存储（第 0 层）的保存结果
 * - 远程存储（第 1..N 层）中第一个成功的保存结果
 */
@Value
@Builder
public class ChainSaveOutcome {

    /** 主存储是否保存成功 */
    boolean primarySaved;

    /** 主存储路径 */
    String primaryPath;

    /** 远程存储是否保存成功 */
    boolean remoteSaved;

    /** 远程存储路径 */
    String remotePath;

    /** 远程存储文件系统 ID */
    Long remoteFsId;

    /** 内容字节大小 */
    long contentSize;

    /** 远程存储错误描述 */
    String remoteError;

    /** 仅主存储保存成功（无远程或远程全部失败） */
    public static ChainSaveOutcome primaryOnly(String primaryPath, long contentSize) {
        return ChainSaveOutcome.builder()
                .primarySaved(true)
                .primaryPath(primaryPath)
                .remoteSaved(false)
                .remotePath(null)
                .remoteFsId(null)
                .contentSize(contentSize)
                .remoteError(null)
                .build();
    }

    /** 主存储 + 远程存储均保存成功 */
    public static ChainSaveOutcome dual(
            String primaryPath, String remotePath, Long remoteFsId, long contentSize) {
        return ChainSaveOutcome.builder()
                .primarySaved(true)
                .primaryPath(primaryPath)
                .remoteSaved(true)
                .remotePath(remotePath)
                .remoteFsId(remoteFsId)
                .contentSize(contentSize)
                .remoteError(null)
                .build();
    }

    /** 仅远程存储保存成功（用于 saveRemoteOnly） */
    public static ChainSaveOutcome remoteOnly(String remotePath, Long remoteFsId, long contentSize) {
        return ChainSaveOutcome.builder()
                .primarySaved(false)
                .primaryPath(null)
                .remoteSaved(true)
                .remotePath(remotePath)
                .remoteFsId(remoteFsId)
                .contentSize(contentSize)
                .remoteError(null)
                .build();
    }

    /** 全部失败 */
    public static ChainSaveOutcome failed(long contentSize, String error) {
        return ChainSaveOutcome.builder()
                .primarySaved(false)
                .primaryPath(null)
                .remoteSaved(false)
                .remotePath(null)
                .remoteFsId(null)
                .contentSize(contentSize)
                .remoteError(error)
                .build();
    }
}
