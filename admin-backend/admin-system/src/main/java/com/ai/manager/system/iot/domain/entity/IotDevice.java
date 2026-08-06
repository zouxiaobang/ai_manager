package com.ai.manager.system.iot.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IoT 设备实体（iot_device）
 * <p>
 * 对应小智 ESP32 设备的注册/激活记录，ws_token 用于 WebSocket 握手鉴权，
 * status 取值：UNBOUND/BOUND/ACTIVATED/OFFLINE/ONLINE，ota_state 取值：IDLE/UPGRADING/SUCCESS/FAILED。
 * </p>
 */
@Data
@TableName("iot_device")
public class IotDevice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备全局唯一标识（后端签发） */
    private String uuid;

    /** 客户端 UUID（设备固件生成） */
    private String clientId;

    /** MAC 地址（Device-Id 头，小写去冒号） */
    private String mac;

    /** 机型，如 supermini-c3 / kyle-s3-lcd */
    private String model;

    /** 芯片，如 esp32c3 / esp32s3 */
    private String chip;

    /** 当前固件版本号 */
    private String firmwareVersion;

    /** WebSocket 握手 Bearer Token（设备侧从 OTA check 下发） */
    private String wsToken;

    /** 激活时间 */
    private LocalDateTime activatedAt;

    /** 最近在线/上报时间 */
    private LocalDateTime lastSeenAt;

    /** 设备状态 */
    private String status;

    /** 最近一次会话 ID（冗余，便于后台查询） */
    private String sessionId;

    /** OTA 状态 */
    private String otaState;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
