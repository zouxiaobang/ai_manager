package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.NbTodoItem;
import com.ai.manager.system.domain.enums.NbTodoRepeatType;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

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
            case WEEKLY -> nextWeekly(base, parseWeekDays(item.getRepeatDays()), interval);
            case MONTHLY -> nextMonthly(base, parseMonthDays(item.getRepeatDays()), interval);
            case YEARLY -> nextYearly(base, parseYearDays(item.getRepeatDays()), interval);
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
        next.setRepeatDays(source.getRepeatDays());
        next.setRepeatUntil(source.getRepeatUntil());
        next.setSeriesId(source.getSeriesId() != null ? source.getSeriesId() : source.getId());
        next.setSortOrder(source.getSortOrder());
        return next;
    }

    private static LocalDateTime nextWeekly(LocalDateTime base, List<Integer> weekDays, int interval) {
        if (weekDays.isEmpty()) {
            return base.plusWeeks(interval);
        }
        LocalDate baseDate = base.toLocalDate();
        LocalDate anchorWeek = baseDate.with(DayOfWeek.MONDAY);
        LocalTime time = base.toLocalTime();
        for (int offset = 1; offset <= 370; offset++) {
            LocalDate candidateDate = baseDate.plusDays(offset);
            int dayOfWeek = candidateDate.getDayOfWeek().getValue();
            if (!weekDays.contains(dayOfWeek)) {
                continue;
            }
            long weeks = ChronoUnit.WEEKS.between(anchorWeek, candidateDate.with(DayOfWeek.MONDAY));
            if (weeks % interval == 0) {
                return LocalDateTime.of(candidateDate, time);
            }
        }
        return base.plusWeeks(interval);
    }

    private static LocalDateTime nextMonthly(LocalDateTime base, List<Integer> monthDays, int interval) {
        if (monthDays.isEmpty()) {
            return base.plusMonths(interval);
        }
        LocalDate baseDate = base.toLocalDate();
        LocalDate anchorMonth = baseDate.withDayOfMonth(1);
        LocalTime time = base.toLocalTime();
        for (int offset = 1; offset <= 370; offset++) {
            LocalDate candidateDate = baseDate.plusDays(offset);
            int dayOfMonth = candidateDate.getDayOfMonth();
            if (!monthDays.contains(dayOfMonth)) {
                continue;
            }
            long months = ChronoUnit.MONTHS.between(anchorMonth, candidateDate.withDayOfMonth(1));
            if (months % interval == 0) {
                return LocalDateTime.of(candidateDate, time);
            }
        }
        return base.plusMonths(interval);
    }

    private static LocalDateTime nextYearly(LocalDateTime base, List<String> yearDays, int interval) {
        if (yearDays.isEmpty()) {
            return base.plusYears(interval);
        }
        LocalDate baseDate = base.toLocalDate();
        LocalTime time = base.toLocalTime();
        List<LocalDate> candidates = new ArrayList<>();
        for (int year = baseDate.getYear(); year <= baseDate.getYear() + interval; year++) {
            for (String token : yearDays) {
                LocalDate parsed = parseYearDayToken(year, token);
                if (parsed != null) {
                    candidates.add(parsed);
                }
            }
        }
        Collections.sort(candidates);
        for (LocalDate candidateDate : candidates) {
            if (candidateDate.isAfter(baseDate)) {
                return LocalDateTime.of(candidateDate, time);
            }
        }
        String first = yearDays.get(0);
        LocalDate fallback = parseYearDayToken(baseDate.getYear() + interval, first);
        return fallback == null ? base.plusYears(interval) : LocalDateTime.of(fallback, time);
    }

    private static LocalDateTime nextOccurrence(LocalDateTime base, NbTodoItem item) {
        NbTodoRepeatType type = NbTodoRepeatType.fromValue(item.getRepeatType());
        int interval = normalizeInterval(item.getRepeatInterval());
        return switch (type) {
            case DAILY -> base.plusDays(interval);
            case WEEKLY -> nextWeekly(base, parseWeekDays(item.getRepeatDays()), interval);
            case MONTHLY -> nextMonthly(base, parseMonthDays(item.getRepeatDays()), interval);
            case YEARLY -> nextYearly(base, parseYearDays(item.getRepeatDays()), interval);
            default -> null;
        };
    }

    private static List<Integer> parseWeekDays(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        TreeSet<Integer> days = new TreeSet<>();
        for (String part : raw.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 1 && value <= 7) {
                    days.add(value);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return new ArrayList<>(days);
    }

    private static List<Integer> parseMonthDays(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        TreeSet<Integer> days = new TreeSet<>();
        for (String part : raw.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 1 && value <= 31) {
                    days.add(value);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return new ArrayList<>(days);
    }

    private static List<String> parseYearDays(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        TreeSet<String> days = new TreeSet<>();
        for (String part : raw.split(",")) {
            String token = part.trim();
            if (token.matches("\\d{2}-\\d{2}")) {
                days.add(token);
            }
        }
        return new ArrayList<>(days);
    }

    private static LocalDate parseYearDayToken(int year, String token) {
        String[] parts = token.split("-");
        if (parts.length != 2) {
            return null;
        }
        try {
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return null;
            }
            YearMonth yearMonth = YearMonth.of(year, month);
            if (day > yearMonth.lengthOfMonth()) {
                return null;
            }
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static int normalizeInterval(Integer interval) {
        return interval == null || interval < 1 ? 1 : interval;
    }
}
