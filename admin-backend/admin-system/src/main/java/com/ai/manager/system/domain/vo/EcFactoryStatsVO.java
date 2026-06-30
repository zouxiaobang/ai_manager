package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class EcFactoryStatsVO {

    private long productionCount;

    private long customerCount;

    private long cartonCount;

    private long enabledCount;

    private long disabledCount;
}
