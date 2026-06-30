package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_outbound_order_line")
public class EcOutboundOrderLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String skuCode;

    private Integer quantity;

    private Integer shippedQuantity;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
