package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_outbound_order")
public class EcOutboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long factoryId;

    private Long customerFactoryId;

    /** DRAFT / CONFIRMED / CANCELLED */
    private String status;

    private String remark;

    /** 创单时间 */
    private LocalDateTime orderTime;

    /** 预出货时间 */
    private LocalDateTime expectedShipTime;

    /** 实际出货时间 */
    private LocalDateTime actualShipTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
