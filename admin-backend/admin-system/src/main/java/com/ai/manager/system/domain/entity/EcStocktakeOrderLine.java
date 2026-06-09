package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_stocktake_order_line")
public class EcStocktakeOrderLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String skuCode;

    private Integer bookQuantity;

    private Integer actualQuantity;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
