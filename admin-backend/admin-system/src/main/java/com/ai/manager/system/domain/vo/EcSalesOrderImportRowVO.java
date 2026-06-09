package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcSalesOrderImportRowVO {

    private Long id;

    private Integer rowNo;

    private String parseStatus;

    private String platformOrderNo;

    private String linkName;

    private String skuSpecName;

    private String matchStatus;

    private Long listingLinkSkuId;

    private BigDecimal manualCostPrice;

    private String platformLineStatus;

    private String lineStatus;

    private String statusMatchStatus;

    /** 导入原始数据中的卖家备注（展示用） */
    private String sellerRemark;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private BigDecimal lineReceivedAmount;

    private String errorMessage;
}
