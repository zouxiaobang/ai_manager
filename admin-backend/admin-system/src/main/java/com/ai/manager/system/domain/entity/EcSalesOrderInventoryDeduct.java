package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_sales_order_inventory_deduct")
public class EcSalesOrderInventoryDeduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long orderLineId;

    private Long shortageId;

    private String skuCode;

    private Long inventoryId;

    private Long inventoryLogId;

    private Integer deductQty;

    private Integer beforeQty;

    private Integer afterQty;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
