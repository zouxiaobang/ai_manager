package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class DailyChecklistStatsVO {

    private List<DailyStatsItem> items;
    private DailyStatsSummary summary;

    @Data
    public static class DailyStatsItem {
        private String date;
        private int totalCount;
        private int completedCount;
        private double completionRate;
        private List<PhaseStatsItem> phaseStats;
    }

    @Data
    public static class PhaseStatsItem {
        private String phaseKey;
        private int totalCount;
        private int completedCount;
        private double completionRate;
    }

    @Data
    public static class DailyStatsSummary {
        private int totalDays;
        private double overallRate;
        private int currentStreak;
        private int bestStreak;
        private int perfectDays;
    }
}
