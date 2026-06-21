package com.ai.manager.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai-manager.note-storage")
public class NoteStorageProperties {

    /**
     * 正文存储类型：BAIDU_PAN（需授权）或 LOCAL（本地文件，开发/降级）
     */
    private String type = "BAIDU_PAN";

    /** 本地文件存储根目录（相对项目或绝对路径） */
    private String localRoot = "uploads/notebook-content";

    /** Redis 正文缓存 TTL（秒） */
    private long cacheTtlSeconds = 3600;
}
