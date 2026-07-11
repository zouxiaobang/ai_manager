package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyChecklistSaveRequest {

    private LocalDate date;

    private List<DailyChecklistItemRequest> items;

    @Data
    public static class DailyChecklistItemRequest {
        private String itemKey;
        private Integer completed;
        private String content;
    }
}
