package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DeploySqlBatchItemVO {
    private int index;
    private String sql;
    private int affectedRows;
}
