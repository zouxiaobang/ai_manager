package com.ai.manager.system.iot.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IoT OTA 记录实体（iot_ota_record）
 * <p>
 * 一次固件下发/升级任务，state 取值：UPGRADING/SUCCESS/FAILED/CANCELED。
 * </p>
 */
@Data
@TableName("iot_ota_record")
public class IotOtaRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备 ID */
    private Long deviceId;

    /** 固件 ID */
    private Long firmwareId;

    /** 升级状态 */
    private String state;

    /** 下载进度 0~100 */
    private Integer progress;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
