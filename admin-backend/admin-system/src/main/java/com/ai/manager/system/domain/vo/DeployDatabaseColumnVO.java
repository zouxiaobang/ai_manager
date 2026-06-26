package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DeployDatabaseColumnVO {
    private String columnName;
    private String columnType;
    private boolean nullable;
    private String columnKey;
    private String columnComment;
    private String columnDefault;
    private int ordinalPosition;
}
