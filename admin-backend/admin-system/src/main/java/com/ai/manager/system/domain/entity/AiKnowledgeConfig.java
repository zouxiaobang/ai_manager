package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库 - 系统配置实体
 *
 * <p>键值 JSON 存储，支持按配置键存取任意配置数据</p>
 */
@Data
@TableName("ai_knowledge_config")
public class AiKnowledgeConfig {

    /** 配置键 */
    @TableId
    private String configKey;

    /** 配置 JSON */
    private String configJson;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
