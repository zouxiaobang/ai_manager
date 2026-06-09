package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EcExpressBillParseSupport {

    public static final String KEY_TRACKING = "tracking_number";
    public static final String KEY_FREIGHT = "freight_amount";
    public static final String KEY_SETTLEMENT_DESTINATION = "settlement_destination";
    public static final String KEY_WEIGHT = "weight";
    public static final String KEY_SHIP_TIME = "ship_time";

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final DateTimeFormatter OUTPUT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private EcExpressBillParseSupport() {
    }

    public record ExpressBillRow(
            String trackingNumber,
            BigDecimal freightAmount,
            String settlementDestination,
            BigDecimal weight,
            LocalDateTime shipTime) {
    }

    /** 合并用户/配置映射与表头自动识别，补全未配置的可选列 */
    public static Map<String, String> enrichColumnMapping(List<String> columns, Map<String, String> mapping) {
        Map<String, String> detected = defaultColumnMapping(columns);
        Map<String, String> merged = new LinkedHashMap<>();
        for (String key : List.of(KEY_TRACKING, KEY_FREIGHT, KEY_SETTLEMENT_DESTINATION, KEY_WEIGHT, KEY_SHIP_TIME)) {
            String configured = mapping != null ? mapping.get(key) : null;
            if (StringUtils.hasText(configured)) {
                merged.put(key, configured.trim());
            } else if (StringUtils.hasText(detected.get(key))) {
                merged.put(key, detected.get(key));
            } else {
                merged.put(key, "");
            }
        }
        return merged;
    }

    public static String normalizeTracking(String tracking) {
        if (!StringUtils.hasText(tracking)) {
            return "";
        }
        String value = tracking.trim().replaceAll("\\s+", "");
        if (value.matches("\\d+\\.0+")) {
            value = value.substring(0, value.indexOf('.'));
        }
        if (value.contains("E") || value.contains("e")) {
            try {
                value = new BigDecimal(value).toPlainString();
                if (value.matches("\\d+\\.0+")) {
                    value = value.substring(0, value.indexOf('.'));
                }
            } catch (NumberFormatException ignored) {
                // keep original
            }
        }
        return value;
    }

    public static List<String> readHeaderColumns(MultipartFile file, int headerRow) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请上传账单文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (name.endsWith(".csv") || name.endsWith(".txt")) {
                return readCsvHeaderColumns(file, headerRow);
            }
            return readExcelHeaderColumns(file, headerRow);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "读取表头失败: " + ex.getMessage());
        }
    }

    public static List<ExpressBillRow> parseRows(MultipartFile file, Map<String, String> columnMapping,
                                                 int headerRow, int dataStartRow) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请上传快递账单文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (name.endsWith(".csv") || name.endsWith(".txt")) {
                return parseCsvRows(file, columnMapping, headerRow, dataStartRow);
            }
            return parseExcelRows(file, columnMapping, headerRow, dataStartRow);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账单解析失败: " + ex.getMessage());
        }
    }

    public static Map<String, String> defaultColumnMapping(List<String> columns) {
        String[] headers = columns.toArray(new String[0]);
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put(KEY_TRACKING, resolveHeader(headers,
                "运单号", "运单号码", "快递单号", "单号", "tracking", "运单编号", "快件单号", "面单号"));
        mapping.put(KEY_FREIGHT, resolveHeader(headers,
                "运费", "金额", "freight", "费用", "应付金额", "结算金额", "快递费", "实付金额", "应付运费", "账单金额", "费用合计"));
        mapping.put(KEY_SETTLEMENT_DESTINATION, resolveHeader(headers,
                "结算目的地", "目的地", "收件城市", "到达地", "目的省份", "目的城市", "派件省份", "派件城市",
                "收件省份", "收件地区", "目的省", "目的市", "收件省", "收件市", "到达省份", "到达城市"));
        mapping.put(KEY_WEIGHT, resolveHeader(headers,
                "重量", "计费重量", "结算重量", "实重", "kg", "公斤", "包裹重量", "计费重", "称重重量", "结算重"));
        mapping.put(KEY_SHIP_TIME, resolveHeader(headers,
                "发货时间", "寄件时间", "揽收时间", "揽件时间", "发件时间", "收件时间", "下单时间", "扫描时间",
                "寄件日期", "揽件日期", "账单时间", "结算时间", "出账时间", "账单日期"));
        return mapping;
    }

    private static List<String> readCsvHeaderColumns(MultipartFile file, int headerRow) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().toList();
            if (lines.size() < headerRow) {
                return List.of();
            }
            return splitCsvLine(lines.get(headerRow - 1));
        }
    }

    private static List<String> readExcelHeaderColumns(MultipartFile file, int headerRow) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(headerRow - 1);
            if (header == null) {
                return List.of();
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            int cols = Math.max(header.getLastCellNum(), 0);
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < cols; i++) {
                columns.add(readCellValue(header.getCell(i), evaluator));
            }
            return columns;
        }
    }

    private static List<ExpressBillRow> parseCsvRows(MultipartFile file, Map<String, String> columnMapping,
                                                     int headerRow, int dataStartRow) throws Exception {
        List<ExpressBillRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().toList();
            if (lines.size() < headerRow) {
                return rows;
            }
            List<String> headerList = splitCsvLine(lines.get(headerRow - 1));
            ColumnIndex idx = resolveColumnIndex(headerList.toArray(new String[0]), columnMapping);
            for (int i = dataStartRow - 1; i < lines.size(); i++) {
                List<String> cells = splitCsvLine(lines.get(i));
                ExpressBillRow row = toRow(cells, idx);
                if (row != null) {
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private static List<ExpressBillRow> parseExcelRows(MultipartFile file, Map<String, String> columnMapping,
                                                       int headerRow, int dataStartRow) throws Exception {
        List<ExpressBillRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row header = sheet.getRow(headerRow - 1);
            if (header == null) {
                return rows;
            }
            int cols = Math.max(header.getLastCellNum(), 0);
            String[] headers = new String[cols];
            for (int i = 0; i < cols; i++) {
                headers[i] = readCellValue(header.getCell(i), evaluator);
            }
            ColumnIndex idx = resolveColumnIndex(headers, columnMapping);
            for (int r = dataStartRow - 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                List<String> cells = readExcelRow(row, cols, evaluator);
                ExpressBillRow billRow = toRow(cells, idx);
                if (billRow != null) {
                    rows.add(billRow);
                }
            }
        }
        return rows;
    }

    private static List<String> readExcelRow(Row row, int minColumns, FormulaEvaluator evaluator) {
        int last = Math.max(Math.max(row.getLastCellNum(), minColumns), 0);
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < last; i++) {
            cells.add(readCellValue(row.getCell(i), evaluator));
        }
        return cells;
    }

    private static String readCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        try {
            CellType type = cell.getCellType();
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
                    ? DATA_FORMATTER.formatCellValue(cell, evaluator).trim()
                    : DATA_FORMATTER.formatCellValue(cell).trim();
            if (StringUtils.hasText(formatted)) {
                return formatted;
            }
            if (type == CellType.NUMERIC || (type == CellType.FORMULA
                    && cell.getCachedFormulaResultType() == CellType.NUMERIC)) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        LocalDateTime ldt = cell.getLocalDateTimeCellValue();
                        return ldt.format(OUTPUT_DATETIME);
                    } catch (Exception ignored) {
                        return DATA_FORMATTER.formatCellValue(cell);
                    }
                }
                double numeric = cell.getNumericCellValue();
                return BigDecimal.valueOf(numeric)
                        .setScale(10, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString();
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private static String readStringCell(Cell cell) {
        try {
            return cell.getRichStringCellValue().getString().trim();
        } catch (Exception ignored) {
            try {
                return cell.getStringCellValue().trim();
            } catch (Exception ignored2) {
                return "";
            }
        }
    }

    private record ColumnIndex(
            int trackingIdx,
            int freightIdx,
            int settlementDestinationIdx,
            int destinationProvinceIdx,
            int destinationCityIdx,
            int weightIdx,
            int shipTimeIdx) {
    }

    private static ColumnIndex resolveColumnIndex(String[] headers, Map<String, String> columnMapping) {
        int trackingIdx = indexOfHeader(headers, columnMapping.get(KEY_TRACKING));
        int freightIdx = indexOfHeader(headers, columnMapping.get(KEY_FREIGHT));
        int settlementDestinationIdx = indexOfHeader(headers, columnMapping.get(KEY_SETTLEMENT_DESTINATION));
        int destinationProvinceIdx = findColumnIndex(headers, "目的省", "收件省", "派件省", "到达省", "目的省份", "收件省份");
        int destinationCityIdx = findColumnIndex(headers, "目的市", "收件市", "派件市", "到达市", "目的城市", "收件城市");
        int weightIdx = indexOfHeader(headers, columnMapping.get(KEY_WEIGHT));
        int shipTimeIdx = indexOfHeader(headers, columnMapping.get(KEY_SHIP_TIME));
        if (trackingIdx < 0) {
            trackingIdx = findColumnIndex(headers, "运单号", "运单号码", "快递单号", "单号", "tracking", "运单编号", "快件单号");
        }
        if (freightIdx < 0) {
            freightIdx = findColumnIndex(headers, "运费", "金额", "freight", "费用", "应付金额", "结算金额", "快递费", "应付运费", "账单金额");
        }
        if (settlementDestinationIdx < 0) {
            settlementDestinationIdx = findColumnIndex(headers,
                    "结算目的地", "目的地", "收件城市", "到达地", "目的省份", "目的城市", "派件省份", "派件城市");
        }
        if (weightIdx < 0) {
            weightIdx = findColumnIndex(headers, "重量", "计费重量", "结算重量", "实重", "kg", "公斤", "计费重", "结算重");
        }
        if (shipTimeIdx < 0) {
            shipTimeIdx = findColumnIndex(headers,
                    "发货时间", "寄件时间", "揽收时间", "揽件时间", "发件时间", "收件时间", "扫描时间",
                    "寄件日期", "揽件日期", "账单时间", "结算时间", "出账时间", "账单日期");
        }
        if (trackingIdx < 0 || freightIdx < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请配置运单号与运费列映射");
        }
        return new ColumnIndex(trackingIdx, freightIdx, settlementDestinationIdx,
                destinationProvinceIdx, destinationCityIdx, weightIdx, shipTimeIdx);
    }

    private static int indexOfHeader(String[] headers, String mappedName) {
        if (!StringUtils.hasText(mappedName)) {
            return -1;
        }
        String target = mappedName.trim();
        for (int i = 0; i < headers.length; i++) {
            if (target.equals(headers[i] == null ? "" : headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private static int findColumnIndex(String[] headers, String... candidates) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i] == null ? "" : headers[i].trim();
            for (String candidate : candidates) {
                if (h.equals(candidate) || h.contains(candidate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String resolveHeader(String[] headers, String... candidates) {
        int idx = findColumnIndex(headers, candidates);
        if (idx < 0) {
            return "";
        }
        return headers[idx] == null ? "" : headers[idx].trim();
    }

    private static ExpressBillRow toRow(List<String> cells, ColumnIndex idx) {
        String tracking = normalizeTracking(cell(cells, idx.trackingIdx()));
        if (!StringUtils.hasText(tracking)) {
            return null;
        }
        BigDecimal freight = SysImportParseSupport.tryParseMoney(cell(cells, idx.freightIdx()));
        if (freight == null) {
            return null;
        }
        String settlementDestination = idx.settlementDestinationIdx() >= 0
                ? trimToNull(cell(cells, idx.settlementDestinationIdx())) : null;
        if (!StringUtils.hasText(settlementDestination)) {
            settlementDestination = combineDestination(cells, idx.destinationProvinceIdx(), idx.destinationCityIdx());
        }
        BigDecimal weight = idx.weightIdx() >= 0 ? tryParseWeight(cell(cells, idx.weightIdx())) : null;
        LocalDateTime shipTime = idx.shipTimeIdx() >= 0
                ? SysImportParseSupport.tryParseDateTime(cell(cells, idx.shipTimeIdx())) : null;
        return new ExpressBillRow(
                tracking,
                freight.setScale(2, RoundingMode.HALF_UP),
                settlementDestination,
                weight,
                shipTime);
    }

    private static BigDecimal tryParseWeight(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("[^0-9.\\-]", "");
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized).setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String combineDestination(List<String> cells, int provinceIdx, int cityIdx) {
        String province = provinceIdx >= 0 ? trimToNull(cell(cells, provinceIdx)) : null;
        String city = cityIdx >= 0 ? trimToNull(cell(cells, cityIdx)) : null;
        if (!StringUtils.hasText(province) && !StringUtils.hasText(city)) {
            return null;
        }
        if (!StringUtils.hasText(province)) {
            return city;
        }
        if (!StringUtils.hasText(city)) {
            return province;
        }
        return province + " " + city;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String cell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        return cells.get(index) == null ? "" : cells.get(index);
    }

    private static List<String> splitCsvLine(String line) {
        if (line == null) {
            return List.of();
        }
        String[] parts = line.split(",", -1);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            result.add(part == null ? "" : part.trim());
        }
        return result;
    }
}
