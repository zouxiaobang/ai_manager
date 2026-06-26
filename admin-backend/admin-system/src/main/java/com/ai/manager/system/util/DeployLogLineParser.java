package com.ai.manager.system.util;

import com.ai.manager.system.domain.vo.DeployLogEntryVO;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeployLogLineParser {

    /**
     * Spring Boot 默认格式含可选时区，例如：
     * 2026-06-19T16:46:12.123+08:00  INFO 12345 --- [http-nio-8080-exec-1] c.a.m.f.aspect.ControllerLogAspect : message
     */
    private static final Pattern SPRING_LINE = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?(?:[+-]\\d{2}:?\\d{2}|Z)?)"
                    + "\\s+(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)"
                    + "\\s+\\d+\\s+---\\s+(?:\\[.*?\\]\\s+)*+(\\S+)\\s*:\\s*(.*)$");

    private static final Pattern LEVEL_IN_HEADER = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?(?:[+-]\\d{2}:?\\d{2}|Z)?"
                    + "\\s+(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\b");

    private DeployLogLineParser() {}

    public static DeployLogEntryVO parse(long lineNumber, String rawLine) {
        DeployLogEntryVO entry = new DeployLogEntryVO();
        entry.setLineNumber(lineNumber);
        entry.setRaw(rawLine);
        if (rawLine == null || rawLine.isBlank()) {
            entry.setLevel("INFO");
            entry.setMessage("");
            return entry;
        }

        Matcher matcher = SPRING_LINE.matcher(rawLine);
        if (matcher.matches()) {
            entry.setTimestamp(matcher.group(1));
            entry.setLevel(matcher.group(2));
            entry.setLogger(matcher.group(3));
            entry.setMessage(matcher.group(4));
            return entry;
        }

        String level = detectLevelFromHeader(rawLine);
        entry.setLevel(level);
        entry.setMessage(rawLine);
        return entry;
    }

    /**
     * 仅根据行首时间戳后的级别标记判断；避免在 JSON 出参里误匹配 "ERROR" 字样。
     */
    private static String detectLevelFromHeader(String rawLine) {
        Matcher headerLevel = LEVEL_IN_HEADER.matcher(rawLine);
        if (headerLevel.find()) {
            return headerLevel.group(1);
        }
        // 无标准行首的续行/非结构化文本，默认 INFO，不按正文关键词猜级别
        return "INFO";
    }
}
