package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class EcSalesOrderImportPreviewVO {

    /** 导入预览解析版本，用于确认后端已部署最新代码（当前 4） */
    private Integer previewParserVersion = 4;

    private Long batchId;

    private String batchNo;

    private Integer totalRows;

    private Integer matchedRows;

    private Integer unmatchedRows;

    /** 平台状态未映射到系统行状态的行数 */
    private Integer statusUnmatchedRows;

    private Integer errorRows;

    private List<EcSalesOrderImportRowVO> rows;
}
