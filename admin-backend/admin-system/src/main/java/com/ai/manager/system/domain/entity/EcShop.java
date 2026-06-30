package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_shop")
public class EcShop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String nameEn;

    private String avatarUrl;

    private Long platformId;

    private String remark;

    /** 类目/交易佣金 % */
    private BigDecimal categoryCommissionPct;

    /** 基础技术服务费 % */
    private BigDecimal techServiceFeePct;

    /** 支付手续费 % */
    private BigDecimal paymentFeePct;

    /** 推广/广告默认扣点 % */
    private BigDecimal promotionFeePct;

    /** 履约/代发服务费 % */
    private BigDecimal fulfillmentFeePct;

    /** 退货/逆向物流服务费率 % */
    private BigDecimal returnServiceFeePct;

    /** 分期/花呗手续费 % */
    private BigDecimal installmentFeePct;

    /** 活动/大促技术服务费 % */
    private BigDecimal activityServiceFeePct;

    /** 平台年费/软件服务费（元/年） */
    private BigDecimal annualPlatformFee;

    /** 保证金（元） */
    private BigDecimal depositAmount;

    /** 默认单笔运费险（元） */
    private BigDecimal shippingInsuranceFee;

    /** 其他综合扣点 % */
    private BigDecimal otherFeePct;

    private String otherFeeRemark;

    /** 默认收货省份（快递试算） */
    private String defaultReceiveProvince;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
