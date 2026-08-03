package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.EcOrderImportRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 电商销售订单导入解析器
 *
 * <p>从 {@code EcSalesOrderServiceImpl} 中提取的纯解析逻辑（原始导入行 → 结构化字段、平台状态推断、
 * 金额/数量/时间解析、1688 链接解析、行去重与实收均摊），无数据访问、无副作用（除 {@code apply1688LinkSkuParsing}
 * 会就地修正传入的原始行 map）。由 Spring 单例注入 {@link ObjectMapper}，可独立单元测试。</p>
 */
@Component
public class EcSalesOrderImportParser {

    private final ObjectMapper objectMapper;

    public EcSalesOrderImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 从字符串 map 中按候选 key 顺序取第一个有值的字段并 trim */
    public String getMapValue(Map<String, String> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key) && StringUtils.hasText(map.get(key))) {
                return map.get(key).trim();
            }
        }
        return null;
    }

    /** 从对象 map 中按候选 key 顺序取第一个有值的字段并 trim */
    public String getMapValueFromObject(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && StringUtils.hasText(String.valueOf(val))) {
                return String.valueOf(val).trim();
            }
        }
        return null;
    }

    /** 金额解析：清除非数字符号后转 BigDecimal，无法解析返回 null */
    public BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = SysImportParseSupport.normalizeMoneyText(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 日期解析：委托 SysImportParseSupport 尝试多格式解析 */
    public LocalDateTime parseImportDateTime(Map<String, Object> map, String... keys) {
        String value = getMapValueFromObject(map, keys);
        return SysImportParseSupport.tryParseDateTime(value);
    }

    /**
     * 把导入行实体上的 raw_json 还原为字符串 map（仅保留非空值并 trim）。
     * JSON 损坏时返回空 map，避免影响后续解析。
     */
    public Map<String, String> readImportRowRawAsStringMap(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null && StringUtils.hasText(String.valueOf(entry.getValue()))) {
                    result.put(entry.getKey(), String.valueOf(entry.getValue()).trim());
                }
            }
            return result;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /** 读取平台行状态：优先 platform_line_status，其次 platform_status */
    public String readImportPlatformStatus(Map<String, String> raw) {
        String platformLineStatus = getMapValue(raw, "platform_line_status", "platformLineStatus");
        if (!StringUtils.hasText(platformLineStatus)) {
            platformLineStatus = getMapValue(raw, "platform_status", "platformStatus");
        }
        return platformLineStatus;
    }

    /** 从导入行读取平台状态：已存列值优先，否则从 raw_json 推断 */
    public String readImportPlatformStatusFromRow(EcOrderImportRow row) {
        if (StringUtils.hasText(row.getPlatformLineStatus())) {
            return row.getPlatformLineStatus().trim();
        }
        Map<String, String> raw = readImportRowRawAsStringMap(row);
        return readImportPlatformStatus(raw);
    }

    /** 平台状态列缺失时，根据时间/物流字段推断行状态（完成 > 发货 > 付款） */
    public String inferLineStatusFromRaw(Map<String, String> raw) {
        if (StringUtils.hasText(getMapValue(raw, "complete_time", "completeTime"))) {
            return "COMPLETED";
        }
        if (StringUtils.hasText(getMapValue(raw, "ship_time", "shipTime"))
                || StringUtils.hasText(getMapValue(raw, "tracking_number", "trackingNumber"))) {
            return "SHIPPED";
        }
        if (StringUtils.hasText(getMapValue(raw, "pay_time", "payTime"))) {
            return "PAID";
        }
        return null;
    }

    /** 合并两条错误消息：一条为空取另一条；互相包含取较长者；否则以「；」拼接 */
    public String mergeImportRowMessages(String primary, String secondary) {
        if (!StringUtils.hasText(primary)) {
            return secondary;
        }
        if (!StringUtils.hasText(secondary)) {
            return primary;
        }
        if (primary.contains(secondary) || secondary.contains(primary)) {
            return primary.length() >= secondary.length() ? primary : secondary;
        }
        return primary + "；" + secondary;
    }

    /** 解析导入行卖家备注（raw_json 中 seller_remark 字段），无则返回 null */
    public String parseSellerRemarkFromImportRow(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            return trimToNull(getMapValueFromObject(map, "seller_remark", "sellerRemark"));
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 解析导入行 SKU 数量（raw_json 中 sku_quantity/skuQuantity/quantity），无效返回 null */
    public Integer parseSkuQuantityFromImportRow(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            String qty = getMapValueFromObject(map, "sku_quantity", "skuQuantity", "quantity");
            if (!StringUtils.hasText(qty)) {
                return null;
            }
            String normalized = qty.trim();
            if (normalized.contains(".")) {
                normalized = normalized.substring(0, normalized.indexOf('.'));
            }
            int value = Integer.parseInt(normalized);
            return value > 0 ? value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 解析导入行实收金额（raw_json 中 received_amount 或支付详情），无则返回 null */
    public BigDecimal parseOrderReceivedFromImportRow(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            return parseOrderReceivedFromRaw(toStringMap(map));
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 订单实收：优先 received_amount，其次支付详情中的金额。 */
    public BigDecimal parseOrderReceivedFromRaw(Map<String, String> raw) {
        if (raw == null) {
            return null;
        }
        String value = getMapValue(raw, "received_amount", "receivedAmount");
        if (!StringUtils.hasText(value)) {
            value = SysImportParseSupport.extractAmountFromPayDetail(
                    getMapValue(raw, "pay_detail", "payDetail"));
        }
        return parseDecimal(value);
    }

    /** 解析导入行数量，无法解析时默认 1（最少 1） */
    public int parseImportQty(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return 1;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            String qty = getMapValueFromObject(map, "sku_quantity", "skuQuantity", "quantity");
            if (StringUtils.hasText(qty)) {
                return Math.max(1, Integer.parseInt(qty.trim()));
            }
        } catch (Exception ignored) {
            /* default 1 */
        }
        return 1;
    }

    /** 解析导入行平台行状态：已存列值优先，否则从 raw_json 推断 */
    public String parseImportPlatformLineStatus(EcOrderImportRow row) {
        if (StringUtils.hasText(row.getPlatformLineStatus())) {
            return row.getPlatformLineStatus().trim();
        }
        return readImportPlatformStatus(readImportRowRawAsStringMap(row));
    }

    /** 收集 1688 订单中带卖家备注的平台订单号集合 */
    public Set<String> collect1688OrdersWithSellerRemark(List<Map<String, String>> rows) {
        Set<String> result = new LinkedHashSet<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, String> raw : rows) {
            String orderNo = getMapValue(raw, "platform_order_no", "platformOrderNo");
            if (!StringUtils.hasText(orderNo)) {
                continue;
            }
            if (StringUtils.hasText(getMapValue(raw, "seller_remark", "sellerRemark"))) {
                result.add(orderNo.trim());
            }
        }
        return result;
    }

    /** 1688 订单带卖家备注且行已匹配时，要求人工填写成本（改判 UNMATCHED 的前置判断） */
    public boolean requiresManualCostForSellerRemark(EcOrderImportRow entity, Map<String, String> raw,
                                                     Set<String> sellerRemarkOrders) {
        if (!"OK".equals(entity.getParseStatus())) {
            return false;
        }
        if (StringUtils.hasText(entity.getPlatformOrderNo())
                && sellerRemarkOrders.contains(entity.getPlatformOrderNo().trim())) {
            return true;
        }
        return StringUtils.hasText(getMapValue(raw, "seller_remark", "sellerRemark"));
    }

    /** 1688：从「货品标题」合并字段解析出链接名称与 SKU 规格，就地写回原始行 map */
    public void apply1688LinkSkuParsing(Map<String, String> raw) {
        String combined = getMapValue(raw, "link_name", "linkName");
        if (!StringUtils.hasText(combined)) {
            return;
        }
        Ec1688ImportLinkNameSupport.ParsedLinkSku parsed = Ec1688ImportLinkNameSupport.parse(combined);
        if (StringUtils.hasText(parsed.linkName())) {
            raw.put("link_name", parsed.linkName());
        }
        if (StringUtils.hasText(parsed.skuSpecName())) {
            raw.put("sku_spec_name", parsed.skuSpecName());
        } else {
            raw.remove("sku_spec_name");
            raw.remove("skuSpecName");
        }
    }

    /**
     * 给导入行应用平台行状态：先精确/包含匹配，失败则按时间字段推断；仍失败标 UNMATCHED 并附加错误消息。
     */
    public void applyImportRowStatus(EcOrderImportRow entity, Map<String, String> raw,
                                     EcImportStatusSupport statusSupport) {
        String platformLineStatus = readImportPlatformStatus(raw);
        entity.setPlatformLineStatus(StringUtils.hasText(platformLineStatus) ? platformLineStatus.trim() : null);
        EcImportStatusSupport.ResolveResult resolved = statusSupport.resolveDetailed(platformLineStatus);
        if (!resolved.matched()) {
            String inferred = inferLineStatusFromRaw(raw);
            if (inferred != null) {
                resolved = new EcImportStatusSupport.ResolveResult(true, inferred, platformLineStatus);
            }
        }
        if (resolved.matched()) {
            entity.setStatusMatchStatus("MATCHED");
            entity.setLineStatus(resolved.lineStatus());
        } else {
            entity.setStatusMatchStatus("UNMATCHED");
            entity.setLineStatus(null);
            String message = StringUtils.hasText(resolved.platformText())
                    ? "平台状态「" + resolved.platformText() + "」未映射，请选择系统状态"
                    : "未识别到平台订单状态，请手动选择系统状态";
            entity.setErrorMessage(mergeImportRowMessages(message, entity.getErrorMessage()));
        }
    }

    /** 标准化人工指定的行状态；不合法时抛业务异常 */
    public String normalizeImportLineStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("PAID", "SHIPPED", "COMPLETED", "CANCELLED", "PARTIAL_REFUND", "REFUNDED", "RETURNED")
                .contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "无效的行状态: " + status);
        }
        return normalized;
    }

    /** 将订单实收均摊到同单各明细行，用于入库时计算行级利润。 */
    public List<BigDecimal> allocateOrderReceivedAmongLines(List<EcOrderImportRow> rows) {
        List<BigDecimal> result = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        BigDecimal orderTotal = null;
        for (EcOrderImportRow row : rows) {
            orderTotal = parseOrderReceivedFromImportRow(row);
            if (orderTotal != null) {
                break;
            }
        }
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            for (int i = 0; i < rows.size(); i++) {
                result.add(null);
            }
            return result;
        }
        int lineCount = rows.size();
        if (lineCount == 1) {
            result.add(orderTotal);
            return result;
        }
        BigDecimal perLine = orderTotal.divide(BigDecimal.valueOf(lineCount), 2, RoundingMode.HALF_UP);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < lineCount - 1; i++) {
            result.add(perLine);
            allocated = allocated.add(perLine);
        }
        result.add(orderTotal.subtract(allocated).setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    /** 导入行去重键：链接 SKU id > 链接+规格 > 行号兜底 */
    public String importLineKey(EcOrderImportRow row) {
        if (row.getListingLinkSkuId() != null) {
            return "sku:" + row.getListingLinkSkuId();
        }
        String link = row.getLinkName() != null ? row.getLinkName().trim() : "";
        String spec = row.getSkuSpecName() != null ? row.getSkuSpecName().trim() : "";
        if (StringUtils.hasText(link) || StringUtils.hasText(spec)) {
            return "link:" + link + "|" + spec;
        }
        return "row:" + row.getRowNo();
    }

    /** 按行去重键去重，保留首次出现的行顺序 */
    public List<EcOrderImportRow> dedupeImportRowsByLineKey(List<EcOrderImportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, EcOrderImportRow> deduped = new LinkedHashMap<>();
        for (EcOrderImportRow row : rows) {
            deduped.put(importLineKey(row), row);
        }
        return new ArrayList<>(deduped.values());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Map<String, String> toStringMap(Map<String, ?> raw) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : raw.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }
}
