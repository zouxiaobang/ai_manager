package com.ai.manager.system.domain.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum NbTodoRepeatType {

    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public static NbTodoRepeatType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return NONE;
        }
        try {
            return NbTodoRepeatType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }

    public boolean isRecurring() {
        return this != NONE;
    }
}
