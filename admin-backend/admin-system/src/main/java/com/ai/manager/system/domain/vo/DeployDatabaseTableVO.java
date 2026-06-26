package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeployDatabaseTableVO {
    private String tableName;
    private String tableComment;
    private String engine;
    private long rowCount;
    private int columnCount;
    private List<DeployDatabaseColumnVO> columns = new ArrayList<>();
}
