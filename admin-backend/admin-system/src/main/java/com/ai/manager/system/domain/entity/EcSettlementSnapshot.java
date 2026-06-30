package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_settlement_snapshot")
public class EcSettlementSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String settlementMonth;

    private Integer expressBillImported;

    private String snapshotJson;

    private LocalDateTime calculatedAt;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
