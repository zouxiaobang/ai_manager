package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_product")
public class EcProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long factoryId;

    private String name;

    private String description;

    /** 退点，百分比，如 5.50 表示 5.5% */
    private BigDecimal rebatePct;

    /** 图片文件名 */
    private String imageName;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
