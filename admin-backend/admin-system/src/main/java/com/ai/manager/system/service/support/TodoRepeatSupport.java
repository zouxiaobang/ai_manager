package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.NbTodoItem;
import com.ai.manager.system.domain.enums.NbTodoRepeatType;

import java.time.Duration;
import java.time.LocalDateTime;

public final class TodoRepeatSupport {

    private TodoRepeatSupport() {
    }

    public static boolean isRecurring(NbTodoItem item) {
        return item != null && NbTodoRepeatType.fromValue(item.getRepeatType()).isRecurring();
    }

    public static LocalDateTime nextDueTime(NbTodoItem item) {
        NbTodoRepeatType type = NbTodoRepeatType.fromValue(item.getRepeatType());
        if (!type.isRecurring()) {
            return null;
        }
        int interval = normalizeInterval(item.getRepeatInterval());
        LocalDateTime base = item.getDueTime() != null ? item.getDueTime() : LocalDateTime.now();
        return switch (type) {
            case DAILY -> base.plusDays(interval);
            case WEEKLY -> base.plusWeeks(interval);
            case MONTHLY -> base.plusMonths(interval);
            case YEARLY -> base.plusYears(interval);
            default -> null;
        };
    }

    public static LocalDateTime nextRemindTime(NbTodoItem item, LocalDateTime nextDue) {
        if (item.getRemindTime() == null) {
            return null;
        }
        if (item.getDueTime() == null || nextDue == null) {
            return nextOccurrence(item.getRemindTime(), item);
        }
        Duration offset = Duration.between(item.getRemindTime(), item.getDueTime());
        return nextDue.plus(offset);
    }

    public static boolean isWithinRepeatUntil(NbTodoItem item, LocalDateTime nextDue) {
        if (nextDue == null || item.getRepeatUntil() == null) {
            return nextDue != null;
        }
        return !nextDue.isAfter(item.getRepeatUntil());
    }

    public static NbTodoItem buildNextOccurrence(NbTodoItem source) {
        LocalDateTime nextDue = nextDueTime(source);
        if (nextDue == null || !isWithinRepeatUntil(source, nextDue)) {
            return null;
        }
        NbTodoItem next = new NbTodoItem();
        next.setContent(source.getContent());
        next.setCompleted(0);
        next.setDueTime(nextDue);
        next.setRemindTime(nextRemindTime(source, nextDue));
        next.setRepeatType(source.getRepeatType());
        next.setRepeatInterval(normalizeInterval(source.getRepeatInterval()));
        next.setRepeatUntil(source.getRepeatUntil());
        next.setSeriesId(source.getSeriesId() != null ? source.getSeriesId() : source.getId());
        next.setSortOrder(source.getSortOrder());
        return next;
    }

    private static LocalDateTime nextOccurrence(LocalDateTime base, NbTodoItem item) {
        NbTodoRepeatType type = NbTodoRepeatType.fromValue(item.getRepeatType());
        int interval = normalizeInterval(item.getRepeatInterval());
        return switch (type) {
            case DAILY -> base.plusDays(interval);
            case WEEKLY -> base.plusWeeks(interval);
            case MONTHLY -> base.plusMonths(interval);
            case YEARLY -> base.plusYears(interval);
            default -> null;
        };
    }

    private static int normalizeInterval(Integer interval) {
        return interval == null || interval < 1 ? 1 : interval;
    }
}
