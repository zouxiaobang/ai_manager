package com.ai.manager.system.service.support;

public final class NoteSyncStatus {

    public static final String SYNCING = "SYNCING";
    public static final String SYNCED = "SYNCED";
    public static final String FAILED = "FAILED";
    /** 本地已保存，等待上传至云盘 */
    public static final String CLOUD_PENDING = "CLOUD_PENDING";
    /** 仅本地保存，云盘同步不可用（授权失效或未开启双写） */
    public static final String LOCAL_ONLY = "LOCAL_ONLY";

    private NoteSyncStatus() {
    }
}
