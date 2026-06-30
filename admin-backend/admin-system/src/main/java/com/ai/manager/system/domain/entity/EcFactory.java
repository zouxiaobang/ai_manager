package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_factory")
public class EcFactory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** PRODUCTION / CUSTOMER / CARTON */
    private String factoryType;

    private String contactName;

    private String contactPhone;

    private String address;

    private String remark;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
