package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.domain.entity.EcOrderImportRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EcSalesOrderImportParser 单元测试
 * 纯解析逻辑，直接构造导入行实体与原始 map 断言字段/状态/金额/数量解析，无需 Spring 上下文。
 */
class EcSalesOrderImportParserTest {

    private EcSalesOrderImportParser parser;

    @BeforeEach
    void setUp() {
        parser = new EcSalesOrderImportParser(new ObjectMapper());
    }

    private EcOrderImportRow row(String rawJson) {
        EcOrderImportRow r = new EcOrderImportRow();
        r.setRawJson(rawJson);
        return r;
    }

    // ---------- 基础 map 取值 ----------

    @Test
    void getMapValue_shouldPickFirstHasTextKeyAndTrim() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("link_name", " 玩具 ");
        raw.put("linkName", " 旧值 ");

        assertThat(parser.getMapValue(raw, "link_name", "linkName")).isEqualTo("玩具");
        assertThat(parser.getMapValue(raw, "missing", "linkName")).isEqualTo("旧值");
        assertThat(parser.getMapValue(raw, "missing")).isNull();
        assertThat(parser.getMapValue(null, "a")).isNull();
    }

    @Test
    void getMapValueFromObject_shouldHandleObjectValues() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("qty", 3);
        raw.put("name", " 张三 ");

        assertThat(parser.getMapValueFromObject(raw, "qty")).isEqualTo("3");
        assertThat(parser.getMapValueFromObject(raw, "name")).isEqualTo("张三");
        assertThat(parser.getMapValueFromObject(raw, "x")).isNull();
    }

    // ---------- 金额 / 时间 ----------

    @Test
    void parseDecimal_shouldNormalizeAndParse() {
        assertThat(parser.parseDecimal(" ¥12.50 ")).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(parser.parseDecimal("12")).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(parser.parseDecimal("abc")).isNull();
        assertThat(parser.parseDecimal("  ")).isNull();
        assertThat(parser.parseDecimal(null)).isNull();
    }

    @Test
    void parseImportDateTime_shouldParseOrReturnNull() {
        Map<String, Object> raw = Map.of("order_time", "2026-08-01 10:00:00");

        assertThat(parser.parseImportDateTime(raw, "order_time", "orderTime")).isNotNull();
        assertThat(parser.parseImportDateTime(Map.of(), "order_time")).isNull();
    }

    // ---------- 原始行 → 结构化解析 ----------

    @Test
    void readImportRowRawAsStringMap_shouldParseJsonAndTrim() {
        EcOrderImportRow r = row("{\"link_name\":\" 玩具 \",\"qty\":\"2\",\"empty\":\"\"}");

        Map<String, String> map = parser.readImportRowRawAsStringMap(r);

        assertThat(map).containsEntry("link_name", "玩具");
        assertThat(map).containsEntry("qty", "2");
        assertThat(map).doesNotContainKey("empty");
    }

    @Test
    void readImportRowRawAsStringMap_shouldReturnEmptyOnBlankOrBrokenJson() {
        assertThat(parser.readImportRowRawAsStringMap(row(null))).isEmpty();
        assertThat(parser.readImportRowRawAsStringMap(row("{broken"))).isEmpty();
    }

    @Test
    void readImportPlatformStatus_shouldPreferLineStatusThenStatus() {
        Map<String, String> line = Map.of("platform_line_status", "已发货", "platform_status", "旧");
        assertThat(parser.readImportPlatformStatus(line)).isEqualTo("已发货");

        Map<String, String> onlyStatus = Map.of("platform_status", "已付款");
        assertThat(parser.readImportPlatformStatus(onlyStatus)).isEqualTo("已付款");

        assertThat(parser.readImportPlatformStatus(Map.of())).isNull();
    }

    @Test
    void readImportPlatformStatusFromRow_shouldPreferStoredThenRawJson() {
        EcOrderImportRow stored = row("{\"platform_status\":\"raw\"}");
        stored.setPlatformLineStatus(" 已发货 ");
        assertThat(parser.readImportPlatformStatusFromRow(stored)).isEqualTo("已发货");

        assertThat(parser.readImportPlatformStatusFromRow(row("{\"platform_line_status\":\"已付款\"}"))).isEqualTo("已付款");
    }

    @Test
    void inferLineStatusFromRaw_shouldPrioritizeCompleteShipPay() {
        assertThat(parser.inferLineStatusFromRaw(Map.of("complete_time", "2026-08-01"))).isEqualTo("COMPLETED");
        assertThat(parser.inferLineStatusFromRaw(Map.of("ship_time", "2026-08-01"))).isEqualTo("SHIPPED");
        assertThat(parser.inferLineStatusFromRaw(Map.of("tracking_number", "SF123"))).isEqualTo("SHIPPED");
        assertThat(parser.inferLineStatusFromRaw(Map.of("pay_time", "2026-08-01"))).isEqualTo("PAID");
        assertThat(parser.inferLineStatusFromRaw(Map.of())).isNull();
    }

    @Test
    void mergeImportRowMessages_shouldPickLongerWhenContainsElseJoin() {
        assertThat(parser.mergeImportRowMessages("abc", "abc；def")).isEqualTo("abc；def");
        assertThat(parser.mergeImportRowMessages("A", "B")).isEqualTo("A；B");
        assertThat(parser.mergeImportRowMessages("A", null)).isEqualTo("A");
        assertThat(parser.mergeImportRowMessages(null, "B")).isEqualTo("B");
    }

    @Test
    void parseSellerRemarkFromImportRow_shouldParseAndTrim() {
        assertThat(parser.parseSellerRemarkFromImportRow(row("{\"seller_remark\":\" 备注A \"}"))).isEqualTo("备注A");
        assertThat(parser.parseSellerRemarkFromImportRow(row("{}"))).isNull();
        assertThat(parser.parseSellerRemarkFromImportRow(row(null))).isNull();
    }

    @Test
    void parseSkuQuantityFromImportRow_shouldHandleDecimalAndInvalid() {
        assertThat(parser.parseSkuQuantityFromImportRow(row("{\"sku_quantity\":\"2.9\"}"))).isEqualTo(2);
        assertThat(parser.parseSkuQuantityFromImportRow(row("{\"skuQuantity\":\"3\"}"))).isEqualTo(3);
        assertThat(parser.parseSkuQuantityFromImportRow(row("{\"sku_quantity\":\"0\"}"))).isNull();
        assertThat(parser.parseSkuQuantityFromImportRow(row("{\"sku_quantity\":\"abc\"}"))).isNull();
        assertThat(parser.parseSkuQuantityFromImportRow(row(null))).isNull();
    }

    @Test
    void parseImportQty_shouldDefaultToOneOnAnyFailure() {
        assertThat(parser.parseImportQty(row("{\"sku_quantity\":\"5\"}"))).isEqualTo(5);
        assertThat(parser.parseImportQty(row("{\"sku_quantity\":\"0\"}"))).isEqualTo(1);
        assertThat(parser.parseImportQty(row("{broken"))).isEqualTo(1);
        assertThat(parser.parseImportQty(row(null))).isEqualTo(1);
    }

    @Test
    void parseOrderReceivedFromImportRow_shouldParseAmountOrPayDetail() {
        assertThat(parser.parseOrderReceivedFromImportRow(row("{\"received_amount\":\"99.90\"}")))
                .isEqualByComparingTo(new BigDecimal("99.90"));
        assertThat(parser.parseOrderReceivedFromImportRow(row("{\"pay_detail\":\"金额: 12.34\"}")))
                .isEqualByComparingTo(new BigDecimal("12.34"));
        assertThat(parser.parseOrderReceivedFromImportRow(row("{}"))).isNull();
    }

    @Test
    void parseImportPlatformLineStatus_shouldPreferStoredThenRaw() {
        EcOrderImportRow stored = row("{\"platform_line_status\":\"raw\"}");
        stored.setPlatformLineStatus("已发货");
        assertThat(parser.parseImportPlatformLineStatus(stored)).isEqualTo("已发货");

        assertThat(parser.parseImportPlatformLineStatus(row("{\"platform_status\":\"已付款\"}"))).isEqualTo("已付款");
    }

    // ---------- 1688 相关 ----------

    @Test
    void collect1688OrdersWithSellerRemark_shouldCollectTrimmedOrderNos() {
        List<Map<String, String>> rows = List.of(
                Map.of("platform_order_no", " A1 ", "seller_remark", "备注"),
                Map.of("platformOrderNo", "A2", "sellerRemark", "x"),
                Map.of("platform_order_no", "A3"));

        assertThat(parser.collect1688OrdersWithSellerRemark(rows)).containsExactly("A1", "A2");
        assertThat(parser.collect1688OrdersWithSellerRemark(null)).isEmpty();
    }

    @Test
    void requiresManualCostForSellerRemark_shouldRequireWhenRemarkOrInSet() {
        EcOrderImportRow ok = new EcOrderImportRow();
        ok.setParseStatus("OK");
        ok.setPlatformOrderNo("A1");
        ok.setErrorMessage(null);

        assertThat(parser.requiresManualCostForSellerRemark(ok, Map.of(), Set.of("A1"))).isTrue();
        assertThat(parser.requiresManualCostForSellerRemark(ok, Map.of("seller_remark", "有备注"), Set.of())).isTrue();

        EcOrderImportRow error = new EcOrderImportRow();
        error.setParseStatus("ERROR");
        assertThat(parser.requiresManualCostForSellerRemark(error, Map.of("seller_remark", "有备注"), Set.of())).isFalse();
    }

    @Test
    void apply1688LinkSkuParsing_shouldSplitCombinedTitle() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("link_name", "儿童玩具 颜色: 变形弹射车");

        parser.apply1688LinkSkuParsing(raw);

        assertThat(raw.get("link_name")).isEqualTo("儿童玩具");
        assertThat(raw.get("sku_spec_name")).isEqualTo("变形弹射车");
    }

    @Test
    void apply1688LinkSkuParsing_shouldKeepWholeWhenNoPattern() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("link_name", "普通商品");
        raw.put("sku_spec_name", "旧规格");

        parser.apply1688LinkSkuParsing(raw);

        assertThat(raw.get("link_name")).isEqualTo("普通商品");
        assertThat(raw).doesNotContainKey("sku_spec_name");
    }

    // ---------- 状态应用 / 标准化 ----------

    @Test
    void applyImportRowStatus_shouldMarkMatchedOnKnownStatus() {
        EcOrderImportRow entity = row(null);
        EcImportStatusSupport support = EcImportStatusSupport.from(null, new ObjectMapper());

        parser.applyImportRowStatus(entity, Map.of("platform_line_status", "已发货"), support);

        assertThat(entity.getStatusMatchStatus()).isEqualTo("MATCHED");
        assertThat(entity.getLineStatus()).isEqualTo("SHIPPED");
        assertThat(entity.getPlatformLineStatus()).isEqualTo("已发货");
    }

    @Test
    void applyImportRowStatus_shouldInferFromPayTime() {
        EcOrderImportRow entity = row(null);
        EcImportStatusSupport support = EcImportStatusSupport.from(null, new ObjectMapper());

        parser.applyImportRowStatus(entity, Map.of("pay_time", "2026-08-01 10:00:00"), support);

        assertThat(entity.getStatusMatchStatus()).isEqualTo("MATCHED");
        assertThat(entity.getLineStatus()).isEqualTo("PAID");
    }

    @Test
    void applyImportRowStatus_shouldMarkUnmatchedWithMessage() {
        EcOrderImportRow entity = row(null);
        EcImportStatusSupport support = EcImportStatusSupport.from(null, new ObjectMapper());

        parser.applyImportRowStatus(entity, Map.of("platform_line_status", "神秘状态XYZ"), support);

        assertThat(entity.getStatusMatchStatus()).isEqualTo("UNMATCHED");
        assertThat(entity.getLineStatus()).isNull();
        assertThat(entity.getErrorMessage()).contains("未映射");
    }

    @Test
    void normalizeImportLineStatus_shouldNormalizeAndRejectInvalid() {
        assertThat(parser.normalizeImportLineStatus(" shipped ")).isEqualTo("SHIPPED");
        assertThat(parser.normalizeImportLineStatus(null)).isNull();
        assertThat(parser.normalizeImportLineStatus("")).isNull();
        assertThatThrownBy(() -> parser.normalizeImportLineStatus("UNKNOWN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无效的行状态");
    }

    // ---------- 实收均摊 / 去重 ----------

    @Test
    void allocateOrderReceivedAmongLines_shouldAllocateEvenlyWithRemainderToLast() {
        List<EcOrderImportRow> rows = List.of(
                row("{\"received_amount\":\"10.00\"}"),
                row("{}"));

        List<BigDecimal> allocated = parser.allocateOrderReceivedAmongLines(rows);

        assertThat(allocated).hasSize(2);
        assertThat(allocated.get(0)).isEqualByComparingTo("5.00");
        assertThat(allocated.get(1)).isEqualByComparingTo("5.00");
        assertThat(allocated.get(0).add(allocated.get(1))).isEqualByComparingTo("10.00");
    }

    @Test
    void allocateOrderReceivedAmongLines_shouldReturnNullsWhenNoAmount() {
        List<BigDecimal> allocated = parser.allocateOrderReceivedAmongLines(List.of(row("{}"), row("{}")));

        assertThat(allocated).hasSize(2).allMatch(java.util.Objects::isNull);
        assertThat(parser.allocateOrderReceivedAmongLines(List.of())).isEmpty();
    }

    @Test
    void importLineKey_shouldUseSkuLinkOrRowNo() {
        EcOrderImportRow withSku = new EcOrderImportRow();
        withSku.setListingLinkSkuId(7L);
        assertThat(parser.importLineKey(withSku)).isEqualTo("sku:7");

        EcOrderImportRow withLink = new EcOrderImportRow();
        withLink.setLinkName("玩具");
        withLink.setSkuSpecName("红");
        assertThat(parser.importLineKey(withLink)).isEqualTo("link:玩具|红");

        EcOrderImportRow fallback = new EcOrderImportRow();
        fallback.setRowNo(3);
        assertThat(parser.importLineKey(fallback)).isEqualTo("row:3");
    }

    @Test
    void dedupeImportRowsByLineKey_shouldKeepLastOccurrenceInFirstSeenOrder() {
        EcOrderImportRow a = new EcOrderImportRow();
        a.setListingLinkSkuId(1L);
        a.setRowNo(1);
        EcOrderImportRow b = new EcOrderImportRow();
        b.setListingLinkSkuId(1L);
        b.setRowNo(2);

        // 同 key 时 LinkedHashMap.put 覆盖为最后一行（原实现语义）
        List<EcOrderImportRow> deduped = parser.dedupeImportRowsByLineKey(List.of(a, b));

        assertThat(deduped).containsExactly(b);
        assertThat(parser.dedupeImportRowsByLineKey(null)).isEmpty();
    }
}
