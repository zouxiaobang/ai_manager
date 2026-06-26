package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeployLogAiAnalyzeVO {
    private String summary;
    private List<String> insights = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private List<DeployLogAiInsightItemVO> items = new ArrayList<>();
    private long analyzedLines;
    private long errorCount;
    private long warnCount;
}
