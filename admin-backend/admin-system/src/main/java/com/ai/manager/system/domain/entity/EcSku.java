package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_sku")
public class EcSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 货号 */
    private String skuCode;

    private String specName;

    /** 退点(百分比)，计算以 SKU 为准 */
    private BigDecimal rebatePct;

    /** 图片文件名 */
    private String imageName;

    /** 匹配纸箱 */
    private Long cartonId;

    @TableField(exist = false)
    private String cartonName;

    private BigDecimal salePrice;

    private BigDecimal productLengthCm;

    private BigDecimal productWidthCm;

    private BigDecimal productHeightCm;

    private BigDecimal cartonLengthCm;

    private BigDecimal cartonWidthCm;

    private BigDecimal cartonHeightCm;

    private BigDecimal cartonGrossWeightKg;

    private BigDecimal cartonNetWeightKg;

    /** 外箱装产品数量 */
    private Integer unitsPerCarton;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
