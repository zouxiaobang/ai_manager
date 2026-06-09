package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_listing_link_sku")
public class EcListingLinkSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long linkId;

    /** 链接侧 SKU 展示名称 */
    private String skuName;

    /** 对应 ec_sku.sku_code，多个英文逗号分隔 */
    private String skuCodes;

    /** 折扣 % */
    private BigDecimal discountPct;

    /** 优惠券金额（元） */
    private BigDecimal couponAmount;

    /** 最低设置金额（元） */
    private BigDecimal minSetAmount;

    /** 成本价格（元，含平台费盈亏平衡口径） */
    private BigDecimal costPrice;

    /** 基础成本 = SKU + 纸箱 + 快递 */
    private BigDecimal baseCostAmount;

    /** 平台费（盈亏平衡口径） */
    private BigDecimal platformFeeAmount;

    /** 真实设置金额（元，可手动填写） */
    private BigDecimal actualSetAmount;

    /** 利润（元）= ((真实设置金额 - 优惠券) × 折扣) - 成本价格 */
    private BigDecimal profit;

    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
