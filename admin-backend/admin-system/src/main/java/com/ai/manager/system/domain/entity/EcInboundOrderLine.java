package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_inbound_order_line")
public class EcInboundOrderLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String skuCode;

    /** 下单数量 */
    private Integer quantity;

    /** 实际收货数量（确认收货时填写） */
    private Integer receivedQuantity;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
