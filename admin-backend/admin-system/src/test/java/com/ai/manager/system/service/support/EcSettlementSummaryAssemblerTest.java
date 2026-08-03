package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSalesOrderLine;
import com.ai.manager.system.domain.entity.EcSettlementOrderDecision;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO.MaxProfitOrder;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO.PendingOrder;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO.ShopSummary;
import com.ai.manager.system.service.support.EcSettlementSummaryAssembler.SettlementAction;
import com.ai.manager.system.service.support.EcSettlementSummaryAssembler.SettlementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EcSettlementSummaryAssembler 单元测试
 * 纯结算计算逻辑，直接构造订单/行/决策实体断言汇总聚合、归属决策、最大利润与待定单 VO。
 */
class EcSettlementSummaryAssemblerTest {

    private final EcSettlementSummaryAssembler assembler = new EcSettlementSummaryAssembler();

    private EcShop shop(Long id) {
        EcShop shop = new EcShop();
        shop.setId(id);
        shop.setName("店铺甲");
        return shop;
    }

    private EcSalesOrder order(Long id, String status) {
        EcSalesOrder order = new EcSalesOrder();
        order.setId(id);
        order.setOrderNo("NO-" + id);
        order.setPlatformOrderNo("PLAT-" + id);
        order.setBuyerName("买家" + id);
        order.setStatus(status);
        order.setReceivedAmount(new BigDecimal("100.00"));
        order.setTotalCostAmount(new BigDecimal("40.00"));
        order.setEstimatedFreightAmount(new BigDecimal("10.00"));
        order.setActualFreightAmount(new BigDecimal("12.00"));
        order.setOrderTime(LocalDateTime.of(2026, 8, 3, 10, 0));
        return order;
    }

    private EcSalesOrderLine line(Long id, String status, String sku, String linkName) {
        EcSalesOrderLine line = new EcSalesOrderLine();
        line.setId(id);
        line.setStatus(status);
        line.setSkuSpecName(sku);
        line.setLinkName(linkName);
        line.setPlatformItemName("平台商品" + id);
        return line;
    }

    private EcSettlementOrderDecision decision(Long orderId, int included) {
        EcSettlementOrderDecision decision = new EcSettlementOrderDecision();
        decision.setOrderId(orderId);
        decision.setIncluded(included);
        return decision;
    }

    private ShopSummary summary(List<EcSalesOrder> orders, Map<Long, EcSettlementOrderDecision> decisionMap) {
        return assembler.buildShopSummary(shop(1L), "店铺甲", Set.of(),
                decisionMap, Map.of(), orders, true);
    }

    // ==================== 归属决策 ====================

    @Test
    void resolveAction_shouldIncludeLossWhenReturnedLine() {
        EcSalesOrder order = order(1L, "COMPLETED");
        SettlementAction action = assembler.resolveAction(order, List.of(line(1L, "RETURNED", "红/L", "链接A")), null);
        assertThat(action.type()).isEqualTo(SettlementType.INCLUDE_LOSS);
    }

    @Test
    void resolveAction_shouldExcludeRefundedOrCancelled() {
        SettlementAction refunded = assembler.resolveAction(order(1L, "REFUNDED"), List.of(), null);
        SettlementAction cancelled = assembler.resolveAction(order(2L, "cancelled"), List.of(), null);
        assertThat(refunded.type()).isEqualTo(SettlementType.EXCLUDE);
        assertThat(cancelled.type()).isEqualTo(SettlementType.EXCLUDE);
    }

    @Test
    void resolveAction_shouldIncludeProfitForShippedOrCompleted() {
        assertThat(assembler.resolveAction(order(1L, "SHIPPED"), List.of(), null).type())
                .isEqualTo(SettlementType.INCLUDE_PROFIT);
        assertThat(assembler.resolveAction(order(2L, "COMPLETED"), List.of(), null).type())
                .isEqualTo(SettlementType.INCLUDE_PROFIT);
    }

    @Test
    void resolveAction_shouldBePendingForDraftWithoutDecision() {
        assertThat(assembler.resolveAction(order(1L, "DRAFT"), List.of(), null).type())
                .isEqualTo(SettlementType.PENDING);
    }

    @Test
    void resolveAction_shouldHonorDecisionForPendingStatus() {
        EcSalesOrder order = order(1L, "PAID");
        assertThat(assembler.resolveAction(order, List.of(), decision(1L, 1)).type())
                .isEqualTo(SettlementType.INCLUDE_PROFIT);
        assertThat(assembler.resolveAction(order, List.of(), decision(1L, 0)).type())
                .isEqualTo(SettlementType.EXCLUDE);
    }

    @Test
    void resolveAction_shouldHonorIncludeDecisionForUnknownStatus() {
        EcSalesOrder order = order(1L, "WEIRD");
        assertThat(assembler.resolveAction(order, List.of(), decision(1L, 1)).type())
                .isEqualTo(SettlementType.INCLUDE_PROFIT);
        assertThat(assembler.resolveAction(order, List.of(), null).type())
                .isEqualTo(SettlementType.EXCLUDE);
    }

    // ==================== 店铺汇总 ====================

    @Test
    void buildShopSummary_shouldAccumulateProfitOrderRevenueAndCost() {
        List<EcSalesOrder> orders = List.of(order(1L, "COMPLETED"), order(2L, "SHIPPED"));
        ShopSummary summary = summary(orders, Map.of());

        // 每单营收 100、成本 40、预估运费 10、实际运费 12；两单合计
        assertThat(summary.getTotalRevenue()).isEqualByComparingTo("200.00");
        assertThat(summary.getEstimatedTotalCost()).isEqualByComparingTo("100.00");
        assertThat(summary.getActualTotalCost()).isEqualByComparingTo("104.00");
        assertThat(summary.getEstimatedTotalProfit()).isEqualByComparingTo("100.00");
        assertThat(summary.getActualTotalProfit()).isEqualByComparingTo("96.00");
        assertThat(summary.getTotalActualFreight()).isEqualByComparingTo("24.00");
        assertThat(summary.getIncludedOrderCount()).isEqualTo(2);
        assertThat(summary.getExcludedOrderCount()).isZero();
        assertThat(summary.getPendingOrderCount()).isZero();
    }

    @Test
    void buildShopSummary_shouldIncludeLossOrderAsIncludedWithoutRevenue() {
        EcSalesOrder loss = order(1L, "COMPLETED");
        List<EcSalesOrderLine> returned = List.of(line(1L, "RETURNED", "红/L", "链接A"));
        ShopSummary summary = assembler.buildShopSummary(shop(1L), "店铺甲", Set.of(),
                Map.of(), Map.of(1L, returned), List.of(loss), true);

        assertThat(summary.getIncludedOrderCount()).isEqualTo(1);
        assertThat(summary.getTotalRevenue()).isEqualByComparingTo("0.00");
        assertThat(summary.getEstimatedTotalCost()).isEqualByComparingTo("50.00");
        assertThat(summary.getActualTotalCost()).isEqualByComparingTo("52.00");
    }

    @Test
    void buildShopSummary_shouldCountExcludedBuyer() {
        EcSalesOrder excluded = order(1L, "COMPLETED");
        excluded.setBuyerName("黑名单买家");
        ShopSummary summary = assembler.buildShopSummary(shop(1L), "店铺甲", Set.of("黑名单买家"),
                Map.of(), Map.of(), List.of(excluded), true);

        assertThat(summary.getExcludedOrderCount()).isEqualTo(1);
        assertThat(summary.getIncludedOrderCount()).isZero();
        assertThat(summary.getTotalRevenue()).isEqualByComparingTo("0.00");
    }

    @Test
    void buildShopSummary_shouldExcludeRefundedOrder() {
        ShopSummary summary = summary(List.of(order(1L, "REFUNDED")), Map.of());
        assertThat(summary.getExcludedOrderCount()).isEqualTo(1);
        assertThat(summary.getIncludedOrderCount()).isZero();
    }

    @Test
    void buildShopSummary_shouldCollectPendingOrdersWithoutDecision() {
        List<EcSalesOrder> orders = List.of(order(1L, "DRAFT"), order(2L, "PAID"));
        ShopSummary summary = summary(orders, Map.of());

        assertThat(summary.getPendingOrderCount()).isEqualTo(2);
        assertThat(summary.getPendingOrders()).hasSize(2);
        assertThat(summary.getPendingOrders().get(0).getDecided()).isFalse();
        assertThat(summary.getIncludedOrderCount()).isZero();
    }

    @Test
    void buildShopSummary_shouldResolvePendingByDecisionAndMarkDecided() {
        List<EcSalesOrder> orders = List.of(order(1L, "PAID"));
        ShopSummary summary = summary(orders, Map.of(1L, decision(1L, 1)));

        assertThat(summary.getPendingOrderCount()).isZero();
        assertThat(summary.getIncludedOrderCount()).isEqualTo(1);
    }

    @Test
    void buildShopSummary_shouldTrackMaxProfitOrder() {
        List<EcSalesOrder> orders = List.of(
                order(1L, "COMPLETED"),
                order(2L, "COMPLETED"));
        // 单 2 预估利润更高：100-40-10=50 > 单 1 的 100-40-10=50 相同，故取第一个
        orders.get(1).setReceivedAmount(new BigDecimal("200.00"));
        ShopSummary summary = summary(orders, Map.of());

        MaxProfitOrder max = summary.getMaxProfitOrder();
        assertThat(max).isNotNull();
        assertThat(max.getOrderId()).isEqualTo(2L);
        assertThat(max.getReceivedAmount()).isEqualByComparingTo("200.00");
    }

    // ==================== 最大利润单 ====================

    @Test
    void buildMaxProfitOrder_shouldMarkActualProfitUnknownWhenBillNotImported() {
        MaxProfitOrder max = assembler.buildMaxProfitOrder(order(1L, "COMPLETED"), List.of(),
                new BigDecimal("100.00"), new BigDecimal("40.00"),
                new BigDecimal("10.00"), new BigDecimal("12.00"),
                new BigDecimal("50.00"), false);

        assertThat(max.getActualProfitAmount()).isNull();
        assertThat(max.getActualProfitUnknownReason()).isEqualTo("EXPRESS_BILL_NOT_IMPORTED");
        assertThat(max.getProfitAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void buildMaxProfitOrder_shouldMarkActualProfitUnknownWhenFreightMissing() {
        EcSalesOrder order = order(1L, "COMPLETED");
        order.setActualFreightAmount(null);
        MaxProfitOrder max = assembler.buildMaxProfitOrder(order, List.of(),
                new BigDecimal("100.00"), new BigDecimal("40.00"),
                new BigDecimal("10.00"), BigDecimal.ZERO,
                new BigDecimal("50.00"), true);

        assertThat(max.getActualProfitAmount()).isNull();
        assertThat(max.getActualProfitUnknownReason()).isEqualTo("ACTUAL_FREIGHT_MISSING");
    }

    @Test
    void buildMaxProfitOrder_shouldComputeActualProfitWhenDataComplete() {
        MaxProfitOrder max = assembler.buildMaxProfitOrder(order(1L, "COMPLETED"), List.of(),
                new BigDecimal("100.00"), new BigDecimal("40.00"),
                new BigDecimal("10.00"), new BigDecimal("12.00"),
                new BigDecimal("50.00"), true);

        // 实际利润 = 100 - 40 - 12 = 48
        assertThat(max.getActualProfitAmount()).isEqualByComparingTo("48.00");
        assertThat(max.getActualProfitUnknownReason()).isNull();
        assertThat(max.getProductName()).isNull();
    }

    @Test
    void buildMaxProfitOrder_shouldJoinLineText() {
        List<EcSalesOrderLine> lines = List.of(
                line(1L, "NORMAL", "红/L", "链接A"),
                line(2L, "NORMAL", "蓝/XL", "链接B"));
        MaxProfitOrder max = assembler.buildMaxProfitOrder(order(1L, "COMPLETED"), lines,
                new BigDecimal("100.00"), new BigDecimal("40.00"),
                new BigDecimal("10.00"), new BigDecimal("12.00"),
                new BigDecimal("50.00"), true);

        assertThat(max.getProductName()).isEqualTo("链接A / 链接B");
        assertThat(max.getSkuName()).isEqualTo("红/L / 蓝/XL");
    }

    // ==================== 待定单 ====================

    @Test
    void toPendingOrder_shouldMarkDecidedWhenDecisionExists() {
        PendingOrder pending = assembler.toPendingOrder(order(1L, "PAID"),
                List.of(line(1L, "NORMAL", "红/L", "链接A")), decision(1L, 1));

        assertThat(pending.getOrderId()).isEqualTo(1L);
        assertThat(pending.getDecided()).isTrue();
        assertThat(pending.getIncluded()).isTrue();
        assertThat(pending.getStatus()).isEqualTo("PAID");
    }

    @Test
    void toPendingOrder_shouldBeUndecidedWhenNoDecision() {
        PendingOrder pending = assembler.toPendingOrder(order(1L, "DRAFT"), List.of(), null);

        assertThat(pending.getDecided()).isFalse();
        assertThat(pending.getIncluded()).isNull();
        assertThat(pending.getProductName()).isNull();
    }
}
