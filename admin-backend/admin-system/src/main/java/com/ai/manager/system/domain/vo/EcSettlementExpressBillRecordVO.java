package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcSettlementExpressBillRecordVO {

    private Long id;

    private String billMonth;

    private Long expressStationId;

    private Boolean otherExpress;

    private String expressStationName;

    private String fileName;

    private String importMode;

    private Integer totalRows;

    private Integer matchedRows;

    private Integer unmatchedRows;

    private Integer gapOrderRows;

    private Integer manualAppliedRows;

    private Boolean includeLabelPrice;

    private String status;

    private LocalDateTime createTime;
}
