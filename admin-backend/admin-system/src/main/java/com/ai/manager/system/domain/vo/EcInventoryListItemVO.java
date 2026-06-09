package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcInventoryListItemVO {

    private Long id;

    private String skuCode;

    private String specName;

    private String productName;

    private Integer quantity;

    /** SKU 售价，用于前端计算存货价值 */
    private BigDecimal salePrice;

    private Boolean ignoreAlert;

    private Integer alertThreshold;

    /** 是否处于预警状态 */
    private Boolean alertActive;

    /** 草稿进货单在途数量 */
    private Integer inTransitQty;

    private LocalDateTime updateTime;

    private List<EcInventoryLogVO> recentLogs;
}
