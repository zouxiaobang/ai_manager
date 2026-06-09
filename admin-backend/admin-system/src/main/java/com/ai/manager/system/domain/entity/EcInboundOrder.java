package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_inbound_order")
public class EcInboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long factoryId;

    /** DRAFT / CONFIRMED / CANCELLED */
    private String status;

    private String remark;

    /** 下单时间 */
    private LocalDateTime orderTime;

    /** 预收货时间 */
    private LocalDateTime expectedDeliveryTime;

    /** 实际收货时间 */
    private LocalDateTime actualReceiptTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
