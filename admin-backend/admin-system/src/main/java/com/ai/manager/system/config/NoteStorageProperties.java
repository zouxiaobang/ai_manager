package com.ai.manager.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai-manager.note-storage")
public class NoteStorageProperties {

    /**
     * 正文存储类型（双写关闭时的降级策略）：BAIDU_PAN 或 LOCAL。
     * 存储中心开启双写后，正文将同时写入本地与百度网盘（storage_type=DUAL）。
     */
    private String type = "BAIDU_PAN";

    /** 本地文件存储根目录（相对项目或绝对路径） */
    private String localRoot = "uploads/notebook-content";

    /** Redis 正文缓存 TTL（秒） */
    private long cacheTtlSeconds = 3600;

    /** Redis 正文缓存总容量上限（MB），0 表示不限制 */
    private long cacheMaxMb = 512;
}
