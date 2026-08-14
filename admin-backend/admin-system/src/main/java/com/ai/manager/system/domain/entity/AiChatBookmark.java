package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库对话标记（跨设备同步）
 */
@Data
@TableName("ai_chat_bookmark")
public class AiChatBookmark {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属对话 ID */
    private Long conversationId;

    /** 标记名称 */
    private String name;

    /** 定位锚点消息 id（记录时视口附近消息，可空） */
    private String msgId;

    /** 锚点消息记录时相对容器内容顶部的偏移 */
    private Integer msgOffsetTop;

    /** 记录时的容器滚动位置 */
    private Integer scrollTop;

    /** 排序 */
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
