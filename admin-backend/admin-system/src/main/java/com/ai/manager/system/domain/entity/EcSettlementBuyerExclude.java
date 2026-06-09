package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_settlement_buyer_exclude")
public class EcSettlementBuyerExclude {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private String buyerName;

    private String remark;

    private Integer enabled;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
