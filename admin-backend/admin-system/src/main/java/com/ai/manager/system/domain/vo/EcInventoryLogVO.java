package com.ai.manager.system.domain.vo;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
public class EcInventoryLogVO {

    private Long id;

    private Long inventoryId;

    private String changeType;

    private Integer changeQty;

    private String refType;

    private Long refId;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
