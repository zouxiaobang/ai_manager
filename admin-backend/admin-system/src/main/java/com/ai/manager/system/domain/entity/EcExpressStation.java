package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_express_station")
public class EcExpressStation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 站点头像，对应 uploads/ecommerce 下文件名 */
    private String avatarUrl;

    private String contact;

    private String address;

    private BigDecimal labelPrice;

    private Integer isDefault;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
