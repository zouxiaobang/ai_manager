package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_inventory_log")
public class EcInventoryLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inventoryId;

    /** DEDUCT 扣除 / RECLAIM 回收 / INBOUND 进货 */
    private String changeType;

    private Integer changeQty;

    private String refType;

    private Long refId;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
