package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_platform")
public class EcPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String nameEn;

    private String avatarUrl;

    /** 与 {@link com.ai.manager.system.domain.enums.EcPlatformCode} 一致 */
    private Integer platformCode;

    /** ONLINE / OFFLINE，与 {@link com.ai.manager.system.domain.enums.EcChannelType} 一致 */
    private String channelType;

    private String remark;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
