package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeploySqlExecuteResultVO {
    private String target;
    private String targetLabel;
    private String sql;
    /** query | update */
    private String statementType;
    private long durationMs;
    private int affectedRows;
    private int rowCount;
    private List<String> columns = new ArrayList<>();
    private List<List<Object>> rows = new ArrayList<>();
    private String message;
    /** 批量 DML 时的语句条数 */
    private int statementCount;
    private List<DeploySqlBatchItemVO> batchItems = new ArrayList<>();
}
