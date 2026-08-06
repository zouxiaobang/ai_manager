package com.ai.manager.system.iot.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IoT 语音会话实体（iot_session）
 * <p>
 * 一次 WebSocket 连接的完整生命周期，session_id 贯穿整个会话，turn_count 记录唤醒轮次。
 * </p>
 */
@Data
@TableName("iot_session")
public class IotSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备 ID */
    private Long deviceId;

    /** 会话 ID（下发设备，贯穿协议） */
    private String sessionId;

    /** 会话开始时间 */
    private LocalDateTime startedAt;

    /** 会话结束时间 */
    private LocalDateTime endedAt;

    /** 会话内唤醒/对话轮次 */
    private Integer turnCount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
