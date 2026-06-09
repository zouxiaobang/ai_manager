package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcInventoryGlobalLogVO {

    private Long id;

    private Long inventoryId;

    private String skuCode;

    private String specName;

    private String productName;

    private Long factoryId;

    private String factoryName;

    private String changeType;

    private Integer changeQty;

    private String refType;

    private Long refId;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
