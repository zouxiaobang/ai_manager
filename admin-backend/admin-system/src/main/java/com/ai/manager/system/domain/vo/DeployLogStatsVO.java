package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class DeployLogStatsVO {
    private String logFile;
    private long todayTotal;
    private long yesterdayTotal;
    private Double todayChangePercent;
    private long errorCount;
    private long yesterdayErrorCount;
    private Double errorChangePercent;
    private long warnCount;
    private long yesterdayWarnCount;
    private Double warnChangePercent;
    private Map<String, Long> levelCounts = new LinkedHashMap<>();
    private List<DeployLogHourlyPointVO> hourlyTrend = new ArrayList<>();
    private List<DeployLogErrorSummaryVO> topErrors = new ArrayList<>();
}
