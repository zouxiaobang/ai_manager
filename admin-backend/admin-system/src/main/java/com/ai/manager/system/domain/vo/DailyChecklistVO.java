package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DailyChecklistVO {

    private String itemKey;

    private Integer completed;

    private String content;
}
