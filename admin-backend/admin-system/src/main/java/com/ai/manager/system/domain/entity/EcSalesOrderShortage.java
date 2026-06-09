package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_sales_order_shortage")
public class EcSalesOrderShortage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long orderLineId;

    private String skuCode;

    private Integer needQty;

    private Integer deductedQty;

    private Integer shortQty;

    private String status;

    private Integer clearedQty;

    private String clearedRefType;

    private Long clearedRefId;

    private LocalDateTime clearedTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
