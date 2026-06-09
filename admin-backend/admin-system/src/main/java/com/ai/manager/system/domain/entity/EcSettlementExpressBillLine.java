package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_settlement_express_bill_line")
public class EcSettlementExpressBillLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long billId;

    private Long expressStationId;

    /** FILE / GAP_ORDER / MANUAL */
    private String source;

    private Long orderId;

    private String platformOrderNo;

    private String orderNo;

    private String trackingNumber;

    private BigDecimal freightAmount;

    private String settlementDestination;

    private BigDecimal weight;

    private LocalDateTime shipTime;

    /** MATCHED / UNMATCHED / PENDING / APPLIED */
    private String matchStatus;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
