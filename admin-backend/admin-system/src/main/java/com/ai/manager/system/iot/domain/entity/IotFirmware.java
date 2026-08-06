package com.ai.manager.system.iot.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IoT 固件实体（iot_firmware）
 * <p>
 * 后台上传的固件包，status 取值：DRAFT/PUBLISHED；force=1 时设备端必须升级。
 * </p>
 */
@Data
@TableName("iot_firmware")
public class IotFirmware {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 固件版本号，如 2.2.1 */
    private String version;

    /** 本地存储路径 */
    private String filePath;

    /** 文件 SHA-256（十六进制小写） */
    private String fileHash;

    /** 文件字节数 */
    private Long size;

    /** 是否强制升级 0/1（列 force_upgrade，避开 MySQL 保留字 FORCE；字段名与列名驼峰对齐避免 AS 别名） */
    @TableField("force_upgrade")
    private Integer forceUpgrade;

    /** 发布状态 DRAFT/PUBLISHED */
    private String status;

    /** 版本说明 */
    private String releaseNote;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
