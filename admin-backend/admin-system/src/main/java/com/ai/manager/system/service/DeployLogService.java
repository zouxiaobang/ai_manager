package com.ai.manager.system.service;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.domain.dto.DeployLogAnalyzeRequest;
import com.ai.manager.system.domain.vo.DeployLogAiAnalyzeVO;
import com.ai.manager.system.domain.vo.DeployLogAiInsightItemVO;
import com.ai.manager.system.domain.vo.DeployLogEntryVO;
import com.ai.manager.system.domain.vo.DeployLogErrorSummaryVO;
import com.ai.manager.system.domain.vo.DeployLogHourlyPointVO;
import com.ai.manager.system.domain.vo.DeployLogStatsVO;
import com.ai.manager.system.domain.vo.DeployLogTailVO;
import com.ai.manager.system.util.DeployLogLineParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeployLogService {

    private static final int MAX_TAIL_LINES = 2000;
    private static final int STATS_SCAN_LINES = 20000;
    private static final int TOP_ERRORS_LIMIT = 10;
    private static final long TAIL_READ_BYTES = 2L * 1024 * 1024;
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH:00");
    private static final DateTimeFormatter DISPLAY_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private final Path backendDir;
    private final String configuredLogFile;
    private final String loggingFileName;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();
    private final Map<String, AtomicBoolean> streamFlags = new ConcurrentHashMap<>();

    public DeployLogService(
            @Value("${ai-manager.deploy.backend-dir:/opt/ai-manager/backend}") String backendDir,
            @Value("${ai-manager.deploy.log-file:}") String configuredLogFile,
            @Value("${logging.file.name:}") String loggingFileName) {
        this.backendDir = Path.of(backendDir).toAbsolutePath().normalize();
        this.configuredLogFile = configuredLogFile == null ? "" : configuredLogFile.trim();
        this.loggingFileName = loggingFileName == null ? "" : loggingFileName.trim();
    }

    public DeployLogTailVO tail(int lines, String levelFilter, String keyword) {
        int requested = clamp(lines, 1, MAX_TAIL_LINES);
        Path logFile = resolveLogFile();
        DeployLogTailVO result = new DeployLogTailVO();
        result.setLogFile(logFile.toString());
        result.setRequestedLines(requested);
        result.setFileExists(Files.isRegularFile(logFile));

        if (!result.isFileExists()) {
            result.setReturnedLines(0);
            return result;
        }

        List<String> rawLines = readTailLines(logFile, requested);
        List<DeployLogEntryVO> entries = new ArrayList<>();
        long startLine = Math.max(1, estimateStartLine(logFile, rawLines.size()));
        for (int i = 0; i < rawLines.size(); i++) {
            DeployLogEntryVO entry = DeployLogLineParser.parse(startLine + i, rawLines.get(i));
            normalizeEntryTimestamp(entry);
            if (!matchesFilter(entry, levelFilter, keyword)) {
                continue;
            }
            entries.add(entry);
        }
        result.setEntries(entries);
        result.setReturnedLines(entries.size());
        return result;
    }

    public DeployLogStatsVO stats(int hours) {
        int windowHours = clamp(hours, 1, 48);
        Path logFile = resolveLogFile();
        DeployLogStatsVO stats = new DeployLogStatsVO();
        stats.setLogFile(logFile.toString());
        stats.setLevelCounts(new LinkedHashMap<>(Map.of(
                "ERROR", 0L,
                "WARN", 0L,
                "INFO", 0L,
                "DEBUG", 0L,
                "TRACE", 0L,
                "OTHER", 0L)));

        if (!Files.isRegularFile(logFile)) {
            initHourlyTrend(stats, windowHours);
            return stats;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime windowStart = LocalDateTime.now(ZoneId.systemDefault()).minusHours(windowHours);
        Map<String, DeployLogHourlyPointVO> hourly = initHourlyMap(windowHours);
        Map<String, DeployLogErrorSummaryVO> errorMap = new LinkedHashMap<>();
        long todayErrors = 0;
        long yesterdayErrors = 0;
        long todayWarns = 0;
        long yesterdayWarns = 0;

        List<String> lines = readTailLines(logFile, STATS_SCAN_LINES);
        for (String raw : lines) {
            DeployLogEntryVO entry = DeployLogLineParser.parse(0, raw);
            String level = resolveEffectiveLevel(entry);
            stats.getLevelCounts().merge(level, 1L, Long::sum);

            LocalDateTime timestamp = parseTimestamp(entry.getTimestamp());
            if (timestamp != null) {
                LocalDate day = timestamp.toLocalDate();
                if (day.equals(today)) {
                    stats.setTodayTotal(stats.getTodayTotal() + 1);
                    if ("ERROR".equals(level)) {
                        todayErrors++;
                    } else if ("WARN".equals(level)) {
                        todayWarns++;
                    }
                } else if (day.equals(yesterday)) {
                    stats.setYesterdayTotal(stats.getYesterdayTotal() + 1);
                    if ("ERROR".equals(level)) {
                        yesterdayErrors++;
                    } else if ("WARN".equals(level)) {
                        yesterdayWarns++;
                    }
                }
                if (!timestamp.isBefore(windowStart)) {
                    String hourKey = timestamp.format(HOUR_FMT);
                    DeployLogHourlyPointVO point = hourly.get(hourKey);
                    if (point != null) {
                        point.setTotal(point.getTotal() + 1);
                        if ("ERROR".equals(level)) {
                            point.setErrorCount(point.getErrorCount() + 1);
                        } else if ("WARN".equals(level)) {
                            point.setWarnCount(point.getWarnCount() + 1);
                        }
                    }
                }
            }

            if ("ERROR".equals(level)) {
                aggregateError(errorMap, entry);
            }
        }

        stats.setErrorCount(todayErrors);
        stats.setYesterdayErrorCount(yesterdayErrors);
        stats.setErrorChangePercent(calcChangePercent(todayErrors, yesterdayErrors));
        stats.setWarnCount(todayWarns);
        stats.setYesterdayWarnCount(yesterdayWarns);
        stats.setWarnChangePercent(calcChangePercent(todayWarns, yesterdayWarns));
        stats.setTodayChangePercent(calcChangePercent(stats.getTodayTotal(), stats.getYesterdayTotal()));

        stats.setHourlyTrend(new ArrayList<>(hourly.values()));
        stats.setTopErrors(errorMap.values().stream()
                .sorted(Comparator.comparingLong(DeployLogErrorSummaryVO::getCount).reversed())
                .limit(TOP_ERRORS_LIMIT)
                .collect(Collectors.toList()));
        return stats;
    }

    public DeployLogAiAnalyzeVO analyze(DeployLogAnalyzeRequest request) {
        int lines = request == null || request.getLines() == null ? 100 : clamp(request.getLines(), 1, MAX_TAIL_LINES);
        DeployLogTailVO tail = tail(lines, null, null);
        DeployLogAiAnalyzeVO result = new DeployLogAiAnalyzeVO();
        result.setAnalyzedLines(tail.getReturnedLines());

        long errors = 0;
        long warns = 0;
        Map<String, Long> errorMessages = new LinkedHashMap<>();
        List<String> recentErrors = new ArrayList<>();

        for (DeployLogEntryVO entry : tail.getEntries()) {
            String level = resolveEffectiveLevel(entry);
            if ("ERROR".equals(level)) {
                errors++;
                String normalized = normalizeMessage(entry.getMessage());
                errorMessages.merge(normalized, 1L, Long::sum);
                if (recentErrors.size() < 5) {
                    recentErrors.add(entry.getMessage());
                }
            } else if ("WARN".equals(level)) {
                warns++;
            }
        }
        result.setErrorCount(errors);
        result.setWarnCount(warns);

        if (!tail.isFileExists()) {
            result.setSummary("未找到后端日志文件，请配置 logging.file.name 或 ai-manager.deploy.log-file。");
            result.getSuggestions().add("在 application-dev.yml 中设置 logging.file.name 指向可写路径后重启后端。");
            result.getSuggestions().add("生产环境可使用 journalctl -u ai-manager-backend -f 查看 systemd 日志。");
            return result;
        }

        if (tail.getReturnedLines() == 0) {
            result.setSummary("日志文件为空或当前筛选无匹配记录。");
            result.getSuggestions().add("触发一次业务请求后再刷新日志。");
            return result;
        }

        String question = request == null ? "" : nullToEmpty(request.getQuestion());
        if (!question.isBlank()) {
            result.setSummary(answerQuestion(question, tail.getEntries(), errors, warns));
        } else if (errors == 0 && warns == 0) {
            result.setSummary(String.format(
                    Locale.ROOT,
                    "最近 %d 条日志未发现 ERROR/WARN，系统运行较为平稳。",
                    tail.getReturnedLines()));
        } else {
            result.setSummary(String.format(
                    Locale.ROOT,
                    "最近 %d 条日志中发现 ERROR %d 条、WARN %d 条，建议优先处理重复异常。",
                    tail.getReturnedLines(),
                    errors,
                    warns));
        }

        errorMessages.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(item -> result.getInsights().add(
                        String.format("重复异常 ×%d：%s", item.getValue(), truncate(item.getKey(), 120))));

        for (String line : recentErrors) {
            result.getInsights().add("最近错误：" + truncate(line, 160));
        }

        if (errors > 0) {
            result.getSuggestions().add("在日志控制台筛选 ERROR 级别，定位最早出现时间与服务模块。");
            result.getSuggestions().add("若为数据库连接失败，检查 MySQL/Redis 与 application.yml 配置。");
        }
        if (warns > 0 && errors == 0) {
            result.getSuggestions().add("当前以 WARN 为主，可观察是否持续增长再决定是否介入。");
        }
        if (errors == 0 && warns == 0) {
            result.getSuggestions().add("保持定期查看 ERROR 统计与近 24 小时趋势即可。");
        }

        result.setItems(buildAiInsightItems(result, errorMessages, recentErrors, errors, warns));
        return result;
    }

    public SseEmitter stream(String levelFilter, String keyword) {
        Path logFile = resolveLogFile();
        if (!Files.isRegularFile(logFile)) {
            throw new BusinessException(404, "日志文件不存在: " + logFile);
        }

        SseEmitter emitter = new SseEmitter(0L);
        String streamId = logFile.toString() + "#" + System.nanoTime();
        AtomicBoolean running = new AtomicBoolean(true);
        streamFlags.put(streamId, running);

        emitter.onCompletion(() -> running.set(false));
        emitter.onTimeout(() -> running.set(false));
        emitter.onError(ex -> running.set(false));

        streamExecutor.execute(() -> {
            try {
                long position = Files.size(logFile);
                sendEvent(emitter, "ready", "{\"connected\":true}");
                while (running.get()) {
                    if (!Files.isRegularFile(logFile)) {
                        Thread.sleep(1000);
                        continue;
                    }
                    long size = Files.size(logFile);
                    if (size > position) {
                        List<String> newLines = readFromPosition(logFile, position, size);
                        position = size;
                        long lineNo = Math.max(1, estimateStartLine(logFile, STATS_SCAN_LINES));
                        for (String raw : newLines) {
                            DeployLogEntryVO entry = DeployLogLineParser.parse(lineNo++, raw);
                            normalizeEntryTimestamp(entry);
                            if (!matchesFilter(entry, levelFilter, keyword)) {
                                continue;
                            }
                            sendEvent(emitter, "log", toJsonLine(entry));
                        }
                    } else if (size < position) {
                        position = 0;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                log.debug("Log stream closed: {}", ex.getMessage());
            } finally {
                streamFlags.remove(streamId);
                emitter.complete();
            }
        });
        return emitter;
    }

    private Path resolveLogFile() {
        if (!configuredLogFile.isBlank()) {
            return Path.of(configuredLogFile).toAbsolutePath().normalize();
        }
        if (!loggingFileName.isBlank()) {
            Path fromLogging = Path.of(loggingFileName).toAbsolutePath().normalize();
            if (Files.isRegularFile(fromLogging)) {
                return fromLogging;
            }
        }
        Path parentDir = backendDir.getParent() != null ? backendDir.getParent() : backendDir;
        Path repoBackendDir = Path.of("admin-backend/.deploy").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                backendDir.resolve("logs/spring.log"),
                parentDir.resolve("deploy/logs/spring.log"),
                parentDir.resolve(".deploy/logs/spring.log"),
                repoBackendDir.resolve("logs/spring.log"),
                backendDir.resolve("logs/application.log"),
                backendDir.resolve("spring.log"),
                Path.of("logs/spring.log").toAbsolutePath().normalize(),
                Path.of("admin-backend/.deploy/logs/spring.log").toAbsolutePath().normalize(),
                Path.of("admin-backend/deploy/logs/spring.log").toAbsolutePath().normalize());
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        if (!loggingFileName.isBlank()) {
            return Path.of(loggingFileName).toAbsolutePath().normalize();
        }
        return candidates.get(0);
    }

    private List<String> readTailLines(Path logFile, int maxLines) {
        try {
            long fileSize = Files.size(logFile);
            long start = Math.max(0, fileSize - TAIL_READ_BYTES);
            byte[] chunk;
            try (RandomAccessFile accessFile = new RandomAccessFile(logFile.toFile(), "r")) {
                accessFile.seek(start);
                chunk = new byte[(int) (fileSize - start)];
                accessFile.readFully(chunk);
            }
            String text = decodeLogBytes(chunk, start > 0);
            List<String> lines = new ArrayList<>();
            for (String line : text.split("\\R")) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            if (lines.size() <= maxLines) {
                return lines;
            }
            return new ArrayList<>(lines.subList(lines.size() - maxLines, lines.size()));
        } catch (IOException ex) {
            throw new BusinessException(500, "读取日志失败: " + ex.getMessage());
        }
    }

    private List<String> readFromPosition(Path logFile, long position, long size) throws IOException {
        int length = (int) Math.min(Integer.MAX_VALUE, size - position);
        if (length <= 0) {
            return List.of();
        }
        byte[] chunk = new byte[length];
        try (RandomAccessFile accessFile = new RandomAccessFile(logFile.toFile(), "r")) {
            accessFile.seek(position);
            accessFile.readFully(chunk);
        }
        String text = decodeLogBytes(chunk, false);
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private long estimateStartLine(Path logFile, int tailCount) {
        try (var lines = Files.lines(logFile, StandardCharsets.UTF_8)) {
            long total = lines.count();
            return Math.max(1, total - tailCount + 1);
        } catch (IOException | UncheckedIOException ex) {
            return 1;
        }
    }

    private boolean matchesFilter(DeployLogEntryVO entry, String levelFilter, String keyword) {
        if (levelFilter != null && !levelFilter.isBlank() && !"ALL".equalsIgnoreCase(levelFilter)) {
            if (!normalizeLevel(entry.getLevel()).equalsIgnoreCase(levelFilter.trim())) {
                return false;
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            String haystack = (entry.getRaw() + " " + entry.getMessage()).toLowerCase(Locale.ROOT);
            if (!haystack.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private void aggregateError(Map<String, DeployLogErrorSummaryVO> errorMap, DeployLogEntryVO entry) {
        String key = normalizeMessage(entry.getMessage());
        DeployLogErrorSummaryVO summary = errorMap.computeIfAbsent(key, ignored -> {
            DeployLogErrorSummaryVO vo = new DeployLogErrorSummaryVO();
            vo.setMessage(truncate(key, 160));
            vo.setCount(0);
            return vo;
        });
        summary.setCount(summary.getCount() + 1);
        summary.setLastSeen(formatDisplayTimestamp(entry.getTimestamp()));
    }

    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "(empty)";
        }
        return NUMBER_PATTERN.matcher(message.trim()).replaceAll("#");
    }

    private static String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "OTHER";
        }
        String upper = level.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "ERROR", "WARN", "INFO", "DEBUG", "TRACE" -> upper;
            default -> "OTHER";
        };
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return null;
        }
        String text = timestamp.trim();
        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // try other formats
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // try other formats
        }
        String normalized = text.replace('T', ' ');
        normalized = normalized.replaceAll("[+-]\\d{2}:?\\d{2}$", "").replaceAll("Z$", "");
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private Map<String, DeployLogHourlyPointVO> initHourlyMap(int hours) {
        Map<String, DeployLogHourlyPointVO> map = new LinkedHashMap<>();
        LocalDateTime cursor = LocalDateTime.now(ZoneId.systemDefault())
                .minusHours(hours - 1L)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        for (int i = 0; i < hours; i++) {
            DeployLogHourlyPointVO point = new DeployLogHourlyPointVO();
            point.setHour(cursor.format(HOUR_FMT));
            map.put(point.getHour(), point);
            cursor = cursor.plusHours(1);
        }
        return map;
    }

    private void initHourlyTrend(DeployLogStatsVO stats, int hours) {
        stats.setHourlyTrend(new ArrayList<>(initHourlyMap(hours).values()));
    }

    private void normalizeEntryTimestamp(DeployLogEntryVO entry) {
        if (entry == null || entry.getTimestamp() == null || entry.getTimestamp().isBlank()) {
            return;
        }
        String formatted = formatDisplayTimestamp(entry.getTimestamp());
        if (!formatted.isBlank()) {
            entry.setTimestamp(formatted);
        }
    }

    private String formatDisplayTimestamp(String timestamp) {
        LocalDateTime parsed = parseTimestamp(timestamp);
        if (parsed == null) {
            return timestamp == null ? "" : timestamp.trim();
        }
        return parsed.format(DISPLAY_TIME_FMT);
    }

    private static Double calcChangePercent(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : 100.0;
        }
        return ((current - previous) * 100.0) / previous;
    }

    private String decodeLogBytes(byte[] chunk, boolean skipPartialFirstLine) {
        String text = decodeWithCharset(chunk, StandardCharsets.UTF_8);
        if (text == null) {
            text = decodeWithCharset(chunk, Charset.forName("GBK"));
        }
        if (text == null) {
            text = new String(chunk, StandardCharsets.UTF_8);
        }
        if (skipPartialFirstLine) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline >= 0) {
                text = text.substring(firstNewline + 1);
            }
        }
        return text;
    }

    private String decodeWithCharset(byte[] chunk, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(chunk)).toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    private List<DeployLogAiInsightItemVO> buildAiInsightItems(
            DeployLogAiAnalyzeVO result,
            Map<String, Long> errorMessages,
            List<String> recentErrors,
            long errors,
            long warns) {
        List<DeployLogAiInsightItemVO> items = new ArrayList<>();

        if (errors > 0) {
            errorMessages.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .forEach(item -> items.add(aiItem(
                            "error",
                            String.format("ERROR 重复异常 ×%d：%s", item.getValue(), truncate(item.getKey(), 120)))));
            for (String line : recentErrors) {
                items.add(aiItem("error", "ERROR 最近错误：" + truncate(line, 160)));
            }
        } else {
            items.add(aiItem("success", "未发现异常错误模式，系统运行健康。"));
        }

        if (warns > 0) {
            items.add(aiItem("warn", String.format("WARN 警告共 %d 条，建议关注是否持续增长。", warns)));
        }

        for (String suggestion : result.getSuggestions()) {
            items.add(aiItem("info", suggestion));
        }

        if (errors == 0 && warns == 0 && result.getSuggestions().isEmpty()) {
            items.add(aiItem("info", "保持定期查看 ERROR 统计与近 24 小时趋势即可。"));
        }

        items.sort(Comparator.comparingInt(item -> severityRank(item.getSeverity())));
        return items;
    }

    private static DeployLogAiInsightItemVO aiItem(String severity, String text) {
        DeployLogAiInsightItemVO item = new DeployLogAiInsightItemVO();
        item.setSeverity(severity);
        item.setText(text);
        return item;
    }

    private static int severityRank(String severity) {
        if (severity == null) {
            return 99;
        }
        return switch (severity.toLowerCase(Locale.ROOT)) {
            case "error" -> 0;
            case "warn" -> 1;
            case "info" -> 2;
            case "success" -> 3;
            default -> 4;
        };
    }

    private String answerQuestion(
            String question, List<DeployLogEntryVO> entries, long errors, long warns) {
        String q = question.toLowerCase(Locale.ROOT);
        if (q.contains("error") || q.contains("错误") || q.contains("异常")) {
            if (errors == 0) {
                return "最近分析的日志中未发现 ERROR 级别记录。";
            }
            return String.format("共发现 %d 条 ERROR，请结合下方重复异常列表定位根因。", errors);
        }
        if (q.contains("warn") || q.contains("警告")) {
            return String.format("共发现 WARN %d 条、ERROR %d 条。", warns, errors);
        }
        if (q.contains("多少") || q.contains("几条")) {
            return String.format("当前分析了 %d 条日志，其中 ERROR %d 条、WARN %d 条。", entries.size(), errors, warns);
        }
        return String.format(
                "已基于最近 %d 条日志进行分析：ERROR %d 条、WARN %d 条。可尝试更具体的问题，例如「有哪些数据库错误」。",
                entries.size(),
                errors,
                warns);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            log.debug("Log stream send failed: {}", ex.getMessage());
        }
    }

    private String toJsonLine(DeployLogEntryVO entry) {
        return "{"
                + "\"lineNumber\":" + entry.getLineNumber() + ","
                + "\"timestamp\":\"" + escapeJson(entry.getTimestamp()) + "\","
                + "\"level\":\"" + escapeJson(entry.getLevel()) + "\","
                + "\"logger\":\"" + escapeJson(entry.getLogger()) + "\","
                + "\"message\":\"" + escapeJson(entry.getMessage()) + "\","
                + "\"raw\":\"" + escapeJson(entry.getRaw()) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String resolveEffectiveLevel(DeployLogEntryVO entry) {
        String level = normalizeLevel(entry.getLevel());
        if ("ERROR".equals(level) && isNoiseErrorLog(entry)) {
            return "INFO";
        }
        return level;
    }

    /** 接口 AOP 响应日志里会包含 errorCount / ERROR 字样，不应算作异常。 */
    private static boolean isNoiseErrorLog(DeployLogEntryVO entry) {
        if (entry == null) {
            return false;
        }
        String logger = nullToEmpty(entry.getLogger());
        String message = nullToEmpty(entry.getMessage()) + nullToEmpty(entry.getRaw());
        if (logger.contains("ControllerLogAspect") && message.contains("接口响应")) {
            return true;
        }
        if (logger.contains("DeployLogController") && message.contains("ai-analyze")) {
            return true;
        }
        return false;
    }
}
