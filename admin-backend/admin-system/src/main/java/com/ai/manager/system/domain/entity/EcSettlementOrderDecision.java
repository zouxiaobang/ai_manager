package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_settlement_order_decision")
public class EcSettlementOrderDecision {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private Long orderId;

    /** YYYY-MM */
    private String settlementMonth;

    /** 1纳入 0不纳入 */
    private Integer included;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
