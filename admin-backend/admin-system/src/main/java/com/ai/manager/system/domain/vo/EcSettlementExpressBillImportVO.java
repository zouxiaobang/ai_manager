package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class EcSettlementExpressBillImportVO {

    private Long billId;

    private String billMonth;

    private Long expressStationId;

    private Boolean otherExpress;

    private String expressStationName;

    private Integer totalRows;

    private Integer matchedRows;

    private Integer unmatchedRows;

    private Integer gapOrderRows;

    private Integer manualPendingRows;

    private Integer manualAppliedRows;

    /** 覆盖已有运单明细的行数 */
    private Integer overwrittenRows;
}
