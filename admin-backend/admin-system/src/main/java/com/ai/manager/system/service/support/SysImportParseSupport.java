package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.SysImportProfile;
import com.ai.manager.system.domain.vo.SysImportFieldVO;
import com.ai.manager.system.service.EcSystemSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SysImportParseSupport {

    private static final Pattern PAY_DETAIL_AMOUNT = Pattern.compile("金额[:：]\\s*([0-9]+(?:\\.[0-9]+)?)");

    private static final DateTimeFormatter OUTPUT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter[] DATETIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-M-d HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("M/d/yy H:mm"),
            DateTimeFormatter.ofPattern("M/d/yy"),
    };

    private final ObjectMapper objectMapper;

    private final SysImportColumnMappingSupport columnMappingSupport;

    private final EcSystemSettingsService ecSystemSettingsService;

    private final DataFormatter dataFormatter = new DataFormatter();

    public List<String> detectColumns(MultipartFile file, SysImportProfile profile) throws Exception {
        String fileName = file.getOriginalFilename();
        if (isCsv(fileName, profile)) {
            return detectCsvColumns(file.getInputStream(), profile);
        }
        return detectExcelColumns(file.getInputStream(), profile);
    }

    public List<Map<String, String>> parseRows(MultipartFile file, SysImportProfile profile) throws Exception {
        return parseBytes(file.getBytes(), file.getOriginalFilename(), profile).rows();
    }

    public ImportParseResult parseBytes(byte[] bytes, String fileName, SysImportProfile profile) throws Exception {
        Map<String, String> columnMapping = readColumnMapping(profile);
        List<String> headers;
        List<List<String>> dataRows;
        if (isCsv(fileName, profile)) {
            ParsedTable table = parseCsvTable(new java.io.ByteArrayInputStream(bytes), profile);
            headers = table.headers();
            dataRows = table.rows();
        } else {
            ParsedTable table = parseExcelTable(new java.io.ByteArrayInputStream(bytes), profile);
            headers = table.headers();
            dataRows = table.rows();
        }
        Map<String, Integer> headerIndex = indexHeaders(headers);
        List<Map<String, String>> result = new ArrayList<>();
        for (List<String> row : dataRows) {
            Map<String, String> mapped = mapRow(columnMapping, headerIndex, row);
            fillAmountFallbacks(headers, row, mapped);
            fillPlatformStatusFallback(headers, row, mapped);
            fillDatetimeFallbacks(headers, row, mapped);
            if (mapped.values().stream().anyMatch(StringUtils::hasText)) {
                result.add(mapped);
            }
        }
        fillMissingOrderHeaderFields(result);
        return new ImportParseResult(headers, result);
    }

    public Map<String, String> readColumnMapping(SysImportProfile profile) {
        return columnMappingSupport.readColumnMapping(profile);
    }

    /**
     * 映射/profile 未命中时，按淘宝等导出常见列名直接从单元格取值。
     */
    private void fillAmountFallbacks(List<String> headers, List<String> cells, Map<String, String> mapped) {
        if (headers == null || cells == null || mapped == null) {
            return;
        }
        fillAmountIfBlank(mapped, headers, cells, "received_amount", "买家实付金额", "总金额", "实付款");
        fillAmountFromPayDetail(mapped, headers, cells);
    }

    private void fillPlatformStatusFallback(List<String> headers, List<String> cells, Map<String, String> mapped) {
        if (headers == null || cells == null || mapped == null) {
            return;
        }
        if (StringUtils.hasText(mapped.get("platform_line_status"))) {
            return;
        }
        String status = readCellByHeaderCandidates(headers, cells, "订单状态", "当前订单状态", "交易状态");
        if (!StringUtils.hasText(status) && !StringUtils.hasText(mapped.get("platform_status"))) {
            status = readCellByHeaderCandidates(headers, cells, "状态");
        }
        if (StringUtils.hasText(status)) {
            mapped.put("platform_line_status", status);
            if (!StringUtils.hasText(mapped.get("platform_status"))) {
                mapped.put("platform_status", status);
            }
        }
    }

    private String readCellByHeaderCandidates(List<String> headers, List<String> cells, String... candidates) {
        for (String candidate : candidates) {
            int idx = indexOfHeader(headers, candidate);
            if (idx < 0) {
                idx = indexOfHeaderContains(headers, candidate);
            }
            if (idx >= 0 && idx < cells.size() && StringUtils.hasText(cells.get(idx))) {
                return cells.get(idx).trim();
            }
        }
        return null;
    }

    private void fillDatetimeFallbacks(List<String> headers, List<String> cells, Map<String, String> mapped) {
        if (headers == null || cells == null || mapped == null) {
            return;
        }
        fillDatetimeIfBlank(mapped, headers, cells, "order_time", "买家下单时间", "下单时间");
        fillDatetimeIfBlank(mapped, headers, cells, "pay_time", "买家付款时间", "付款时间", "支付时间");
        fillDatetimeIfBlank(mapped, headers, cells, "ship_time", "发货时间", "卖家发货时间");
        fillDatetimeIfBlank(mapped, headers, cells, "complete_time", "确认收货时间", "交易成功时间", "完成时间", "交易结束时间");
    }

    private void fillDatetimeIfBlank(Map<String, String> mapped, List<String> headers, List<String> cells,
                                     String backendKey, String... candidateHeaders) {
        if (StringUtils.hasText(mapped.get(backendKey))) {
            return;
        }
        for (String candidate : candidateHeaders) {
            int idx = indexOfHeader(headers, candidate);
            if (idx < 0) {
                idx = indexOfHeaderContains(headers, candidate);
            }
            if (idx < 0 || idx >= cells.size()) {
                continue;
            }
            String parsed = formatDateTimeTextConfigured(cells.get(idx));
            if (StringUtils.hasText(parsed)) {
                mapped.put(backendKey, parsed);
                return;
            }
        }
    }

    public static LocalDateTime tryParseDateTime(String value) {
        String formatted = formatDateTimeText(value);
        if (!StringUtils.hasText(formatted)) {
            return null;
        }
        try {
            return LocalDateTime.parse(formatted, OUTPUT_DATETIME);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static String formatDateTimeText(String value) {
        return formatDateTimeTextWithFormatters(value, DATETIME_FORMATTERS);
    }

    private String formatDateTimeTextConfigured(String value) {
        return formatDateTimeTextWithFormatters(value, datetimeFormatters());
    }

    private static String formatDateTimeTextWithFormatters(String value, DateTimeFormatter[] formatters) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim()
                .replace('T', ' ')
                .replace('/', '-');
        if (text.matches("\\d+(\\.\\d+)?")) {
            try {
                double serial = Double.parseDouble(text);
                if (serial >= 30000 && serial <= 60000) {
                    Date date = DateUtil.getJavaDate(serial, false);
                    LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                    return ldt.format(OUTPUT_DATETIME);
                }
            } catch (Exception ignored) {
                /* not excel serial */
            }
        }
        if (text.matches("\\d{4}\\.\\d{1,2}\\.\\d{1,2}.*")) {
            text = text.replace('.', '-');
        }
        for (DateTimeFormatter formatter : formatters) {
            String normalized = text.contains("/") ? text : text.replace('-', '/');
            try {
                LocalDateTime ldt = LocalDateTime.parse(normalized, formatter);
                return ldt.format(OUTPUT_DATETIME);
            } catch (DateTimeParseException ignored) {
                try {
                    LocalDate date = LocalDate.parse(normalized, formatter);
                    return date.atStartOfDay().format(OUTPUT_DATETIME);
                } catch (DateTimeParseException ignored2) {
                    /* try next */
                }
            }
        }
        if (text.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}")) {
            try {
                String[] parts = text.split("[-/]");
                LocalDate date = LocalDate.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
                return date.atStartOfDay().format(OUTPUT_DATETIME);
            } catch (Exception ignored) {
                return null;
            }
        }
        if (text.matches("\\d{4}-\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2}:\\d{2}")) {
            try {
                String[] parts = text.split("\\s+");
                String[] dateParts = parts[0].split("-");
                String[] timeParts = parts[1].split(":");
                LocalDateTime ldt = LocalDateTime.of(
                        Integer.parseInt(dateParts[0]),
                        Integer.parseInt(dateParts[1]),
                        Integer.parseInt(dateParts[2]),
                        Integer.parseInt(timeParts[0]),
                        Integer.parseInt(timeParts[1]),
                        timeParts.length > 2 ? Integer.parseInt(timeParts[2]) : 0);
                return ldt.format(OUTPUT_DATETIME);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    /** 淘宝「支付详情」列常含「金额：44.09」，比「买家实付金额」文本型单元格更可靠 */
    private void fillAmountFromPayDetail(Map<String, String> mapped, List<String> headers, List<String> cells) {
        if (StringUtils.hasText(mapped.get("received_amount"))) {
            return;
        }
        String detail = readField(mapped, "pay_detail", "payDetail");
        if (!StringUtils.hasText(detail)) {
            int idx = indexOfHeader(headers, "支付详情");
            if (idx >= 0 && idx < cells.size()) {
                detail = cells.get(idx);
            }
        }
        if (!StringUtils.hasText(detail)) {
            return;
        }
        if (!StringUtils.hasText(mapped.get("pay_detail"))) {
            mapped.put("pay_detail", detail.trim());
        }
        String amount = extractAmountFromPayDetail(detail);
        if (StringUtils.hasText(amount)) {
            mapped.put("received_amount", amount);
        }
    }

    public static String extractAmountFromPayDetail(String payDetail) {
        if (!StringUtils.hasText(payDetail)) {
            return null;
        }
        Matcher matcher = PAY_DETAIL_AMOUNT.matcher(payDetail);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static BigDecimal tryParseMoney(String value) {
        String cleaned = normalizeMoneyText(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void fillAmountIfBlank(Map<String, String> mapped, List<String> headers, List<String> cells,
                                   String backendKey, String... candidateHeaders) {
        if (StringUtils.hasText(mapped.get(backendKey))) {
            return;
        }
        for (String candidate : candidateHeaders) {
            int idx = indexOfHeader(headers, candidate);
            if (idx < 0) {
                idx = indexOfHeaderContains(headers, candidate);
            }
            if (idx < 0 || idx >= cells.size()) {
                continue;
            }
            BigDecimal parsed = tryParseMoney(cells.get(idx));
            if (parsed != null) {
                mapped.put(backendKey, parsed.toPlainString());
                return;
            }
        }
    }

    private int indexOfHeaderContains(List<String> headers, String keyword) {
        String target = normalizeHeader(keyword);
        if (!StringUtils.hasText(target)) {
            return -1;
        }
        for (int i = 0; i < headers.size(); i++) {
            String header = normalizeHeader(headers.get(i));
            if (header.contains(target) || target.contains(header)) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfHeader(List<String> headers, String name) {
        String target = normalizeHeader(name);
        for (int i = 0; i < headers.size(); i++) {
            if (normalizeHeader(headers.get(i)).equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, String> mapRow(Map<String, String> columnMapping,
                                       Map<String, Integer> headerIndex,
                                       List<String> row) {
        Map<String, String> mapped = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : columnMapping.entrySet()) {
            String backendKey = entry.getKey();
            String docColumn = entry.getValue();
            if (!StringUtils.hasText(docColumn)) {
                mapped.put(backendKey, "");
                continue;
            }
            Integer idx = resolveColumnIndex(headerIndex, docColumn);
            String value = idx != null && idx < row.size() ? nullToEmpty(row.get(idx)) : "";
            mapped.put(backendKey, value);
        }
        return mapped;
    }

    /** 1688 等导出：首行含订单头，续行仅含 SKU 列；向前填充订单号并传播订单级字段。 */
    private void fillMissingOrderHeaderFields(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        forwardFillPlatformOrderNo(rows);
        Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String orderNo = readField(row, "platform_order_no", "platformOrderNo");
            String key = StringUtils.hasText(orderNo) ? orderNo.trim() : "__ROW__" + i;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        for (List<Map<String, String>> group : groups.values()) {
            propagateOrderHeaderFields(group);
        }
    }

    private void forwardFillPlatformOrderNo(List<Map<String, String>> rows) {
        String lastOrderNo = null;
        for (Map<String, String> row : rows) {
            String orderNo = readField(row, "platform_order_no", "platformOrderNo");
            if (StringUtils.hasText(orderNo)) {
                lastOrderNo = orderNo.trim();
                continue;
            }
            if (lastOrderNo != null && hasImportLineContent(row)) {
                row.put("platform_order_no", lastOrderNo);
            }
        }
    }

    private boolean hasImportLineContent(Map<String, String> row) {
        return StringUtils.hasText(readField(row, "link_name", "linkName"))
                || StringUtils.hasText(readField(row, "sku_spec_name", "skuSpecName"));
    }

    private static final String[][] ORDER_HEADER_FIELD_KEYS = {
            {"platform_order_no", "platformOrderNo"},
            {"order_time", "orderTime"},
            {"pay_time", "payTime"},
            {"ship_time", "shipTime"},
            {"complete_time", "completeTime"},
            {"express_station_name", "expressStationName"},
            {"received_amount", "receivedAmount"},
            {"tracking_number", "trackingNumber"},
            {"buyer_name", "buyerName"},
            {"buyer_phone", "buyerPhone"},
            {"receive_address", "receiveAddress"},
            {"platform_status", "platformStatus"},
            {"platform_line_status", "platformLineStatus"},
    };

    private void propagateOrderHeaderFields(List<Map<String, String>> group) {
        if (group == null || group.isEmpty()) {
            return;
        }
        for (String[] keys : ORDER_HEADER_FIELD_KEYS) {
            propagateFieldToGroup(group, keys);
        }
        propagateFieldToGroup(group, new String[]{"pay_detail", "payDetail"});
        propagateOrderReceivedAmount(group);
    }

    private void propagateFieldToGroup(List<Map<String, String>> group, String[] keys) {
        String value = null;
        for (Map<String, String> row : group) {
            String candidate = readField(row, keys);
            if (StringUtils.hasText(candidate)) {
                value = candidate;
                break;
            }
        }
        if (!StringUtils.hasText(value)) {
            return;
        }
        for (Map<String, String> row : group) {
            if (!StringUtils.hasText(readField(row, keys))) {
                row.put(keys[0], value);
            }
        }
    }

    /** 淘宝等导出：买家实付金额常在订单首行有值，同单后续 SKU 行为空；将订单级金额补到同单各行。 */
    private void propagateOrderReceivedAmount(List<Map<String, String>> group) {
        if (group == null || group.isEmpty()) {
            return;
        }
        String orderTotal = null;
        for (Map<String, String> row : group) {
            String received = readField(row, "received_amount", "receivedAmount");
            if (StringUtils.hasText(received)) {
                orderTotal = received;
                break;
            }
        }
        if (!StringUtils.hasText(orderTotal)) {
            for (Map<String, String> row : group) {
                String fromPay = extractAmountFromPayDetail(readField(row, "pay_detail", "payDetail"));
                if (StringUtils.hasText(fromPay)) {
                    orderTotal = fromPay;
                    break;
                }
            }
        }
        if (!StringUtils.hasText(orderTotal)) {
            return;
        }
        String normalizedTotal = normalizeMoney(orderTotal);
        if (!StringUtils.hasText(normalizedTotal)) {
            return;
        }
        for (Map<String, String> row : group) {
            if (!StringUtils.hasText(readField(row, "received_amount", "receivedAmount"))) {
                row.put("received_amount", normalizedTotal);
            }
        }
    }

    private String readField(Map<String, String> row, String... keys) {
        if (row == null) {
            return null;
        }
        for (String key : keys) {
            String value = row.get(key);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeMoney(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = normalizeMoneyText(value);
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    /** 金额文本清洗：全角数字、货币符号、Excel 文本型数字等 */
    public static String normalizeMoneyText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.trim()
                .replace("\uFEFF", "")
                .replace("\u00a0", "")
                .replace("\u200B", "")
                .replace(" ", "")
                .replace("'", "")
                .replace("￥", "")
                .replace("¥", "")
                .replace(",", "")
                .replace("元", "");
        StringBuilder sb = new StringBuilder(cleaned.length());
        for (char c : cleaned.toCharArray()) {
            if (c >= '０' && c <= '９') {
                sb.append((char) ('0' + (c - '０')));
            } else if (c == '．') {
                sb.append('.');
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private Integer resolveColumnIndex(Map<String, Integer> headerIndex, String docColumn) {
        if (!StringUtils.hasText(docColumn) || headerIndex == null || headerIndex.isEmpty()) {
            return null;
        }
        String target = normalizeHeader(docColumn);
        Integer idx = headerIndex.get(target);
        if (idx != null) {
            return idx;
        }
        for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            if (normalizeHeader(entry.getKey()).equals(target)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, Integer> indexHeaders(List<String> headers) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(normalizeHeader(headers.get(i)), i);
        }
        return map;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim()
                .replace("\uFEFF", "")
                .replace("\u00a0", " ")
                .replaceAll("\\s+", "");
    }

    private List<String> detectCsvColumns(InputStream in, SysImportProfile profile) throws Exception {
        return parseCsvTable(in, profile).headers();
    }

    private List<String> detectExcelColumns(InputStream in, SysImportProfile profile) throws Exception {
        return parseExcelTable(in, profile).headers();
    }

    private ParsedTable parseCsvTable(InputStream in, SysImportProfile profile) throws Exception {
        int headerRow = profile.getHeaderRow() != null ? profile.getHeaderRow() : 1;
        int dataStart = profile.getDataStartRow() != null ? profile.getDataStartRow() : headerRow + 1;
        Charset charset = StandardCharsets.UTF_8;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset))) {
            List<String> lines = reader.lines().toList();
            if (lines.size() < headerRow) {
                return new ParsedTable(List.of(), List.of());
            }
            List<String> headers = splitCsvLine(lines.get(headerRow - 1));
            List<List<String>> rows = new ArrayList<>();
            for (int i = dataStart - 1; i < lines.size(); i++) {
                rows.add(splitCsvLine(lines.get(i)));
            }
            return new ParsedTable(headers, rows);
        }
    }

    private List<String> splitCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        if (line == null) {
            return cols;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                cols.add(trimCsvCell(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cols.add(trimCsvCell(current.toString()));
        return cols;
    }

    private String trimCsvCell(String cell) {
        String v = cell == null ? "" : cell.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length() - 1).trim();
        }
        return v;
    }

    private ParsedTable parseExcelTable(InputStream in, SysImportProfile profile) throws Exception {
        int headerRowIdx = (profile.getHeaderRow() != null ? profile.getHeaderRow() : 1) - 1;
        int dataStartIdx = (profile.getDataStartRow() != null ? profile.getDataStartRow() : headerRowIdx + 2) - 1;
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = resolveSheet(workbook, profile.getSheetName());
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row header = sheet.getRow(headerRowIdx);
            if (header == null) {
                return new ParsedTable(List.of(), List.of());
            }
            int columnWidth = Math.max(header.getLastCellNum(), 0);
            for (int r = dataStartIdx; r <= sheet.getLastRowNum(); r++) {
                Row scanRow = sheet.getRow(r);
                if (scanRow != null) {
                    columnWidth = Math.max(columnWidth, scanRow.getLastCellNum());
                }
            }
            List<String> headers = readExcelRow(header, columnWidth, evaluator);
            List<List<String>> rows = new ArrayList<>();
            for (int r = dataStartIdx; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                List<String> cells = readExcelRow(row, columnWidth, evaluator);
                if (cells.stream().anyMatch(StringUtils::hasText)) {
                    rows.add(cells);
                }
            }
            return new ParsedTable(headers, rows);
        }
    }

    private Sheet resolveSheet(Workbook workbook, String sheetName) {
        if (StringUtils.hasText(sheetName)) {
            Sheet sheet = workbook.getSheet(sheetName.trim());
            if (sheet == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工作表不存在: " + sheetName);
            }
            return sheet;
        }
        return workbook.getSheetAt(0);
    }

    private List<String> readExcelRow(Row row) {
        return readExcelRow(row, 0, null);
    }

    /** 按表头列数补齐，避免数据行比表头短时列索引越界读空 */
    private List<String> readExcelRow(Row row, int minColumns, FormulaEvaluator evaluator) {
        int lastFromRow = row.getLastCellNum() > 0 ? row.getLastCellNum() : 0;
        int last = Math.max(Math.max(lastFromRow, minColumns), 0);
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < last; i++) {
            cells.add(readCellValue(row.getCell(i), evaluator));
        }
        return cells;
    }

    private String readCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        try {
            CellType type = cell.getCellType();
            // 淘宝导出常见「数字以文本形式存储」，优先按字符串读取
            if (type == CellType.STRING) {
                String text = readStringCell(cell);
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
            if (type == CellType.FORMULA) {
                CellType cached = cell.getCachedFormulaResultType();
                if (cached == CellType.STRING) {
                    String text = readStringCell(cell);
                    if (StringUtils.hasText(text)) {
                        return text;
                    }
                }
            }
            String formatted = evaluator != null
                    ? normalizeCellText(dataFormatter.formatCellValue(cell, evaluator))
                    : normalizeCellText(dataFormatter.formatCellValue(cell));
            if (StringUtils.hasText(formatted)) {
                return formatted;
            }
            if (type == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        LocalDateTime ldt = cell.getLocalDateTimeCellValue();
                        return ldt.format(OUTPUT_DATETIME);
                    } catch (Exception ignored) {
                        return nullToEmpty(dataFormatter.formatCellValue(cell));
                    }
                }
                double numeric = cell.getNumericCellValue();
                if (numeric >= 30000 && numeric <= 60000) {
                    String parsed = formatDateTimeTextConfigured(String.valueOf(numeric));
                    if (StringUtils.hasText(parsed)) {
                        return parsed;
                    }
                }
                return BigDecimal.valueOf(numeric)
                        .setScale(10, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString();
            }
            if (type == CellType.BOOLEAN) {
                return String.valueOf(cell.getBooleanCellValue());
            }
            if (type == CellType.FORMULA) {
                CellType cached = cell.getCachedFormulaResultType();
                if (cached == CellType.NUMERIC) {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        try {
                            LocalDateTime ldt = cell.getLocalDateTimeCellValue();
                            return ldt.format(OUTPUT_DATETIME);
                        } catch (Exception ignored) {
                            return nullToEmpty(dataFormatter.formatCellValue(cell, evaluator));
                        }
                    }
                    double numeric = cell.getNumericCellValue();
                    if (numeric >= 30000 && numeric <= 60000) {
                        String parsed = formatDateTimeTextConfigured(String.valueOf(numeric));
                        if (StringUtils.hasText(parsed)) {
                            return parsed;
                        }
                    }
                    return BigDecimal.valueOf(numeric)
                            .setScale(10, RoundingMode.HALF_UP)
                            .stripTrailingZeros()
                            .toPlainString();
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private String readStringCell(Cell cell) {
        try {
            return normalizeCellText(cell.getRichStringCellValue().getString());
        } catch (Exception ignored) {
            try {
                return normalizeCellText(cell.getStringCellValue());
            } catch (Exception ignored2) {
                return "";
            }
        }
    }

    /** 清洗单元格文本：全角数字、货币符号、Excel 文本型数字等 */
    private String normalizeCellText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.trim()
                .replace("\uFEFF", "")
                .replace("\u00a0", "")
                .replace("\u200B", "")
                .replace("'", "")
                .replace("\"", "");
        StringBuilder sb = new StringBuilder(cleaned.length());
        for (char c : cleaned.toCharArray()) {
            if (c >= '０' && c <= '９') {
                sb.append((char) ('0' + (c - '０')));
            } else if (c == '．') {
                sb.append('.');
            } else if (c == '，') {
                sb.append(',');
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private boolean isCsv(String fileName, SysImportProfile profile) {
        if (StringUtils.hasText(profile.getFileType()) && "CSV".equalsIgnoreCase(profile.getFileType())) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".csv") || lower.endsWith(".txt");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private DateTimeFormatter[] datetimeFormatters() {
        String preferred = ecSystemSettingsService.resolveOrderImportDateFormat();
        List<DateTimeFormatter> formatters = new ArrayList<>();
        if (StringUtils.hasText(preferred)) {
            try {
                formatters.add(DateTimeFormatter.ofPattern(preferred.trim()));
            } catch (IllegalArgumentException ignored) {
                /* invalid pattern */
            }
        }
        formatters.addAll(List.of(DATETIME_FORMATTERS));
        return formatters.toArray(DateTimeFormatter[]::new);
    }

    public List<String> toColumnList(List<Map<String, String>> rows) {
        Set<String> set = new LinkedHashSet<>();
        if (rows != null) {
            for (Map<String, String> row : rows) {
                set.addAll(row.keySet());
            }
        }
        return new ArrayList<>(set);
    }

    private record ParsedTable(List<String> headers, List<List<String>> rows) {
    }

    public record ImportParseResult(List<String> headers, List<Map<String, String>> rows) {
    }
}
