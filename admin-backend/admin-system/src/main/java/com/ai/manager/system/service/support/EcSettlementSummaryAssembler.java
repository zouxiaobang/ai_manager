package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSalesOrderLine;
import com.ai.manager.system.domain.entity.EcSettlementOrderDecision;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO.MaxProfitOrder;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO.PendingOrder;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO.ShopSummary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 电商月结店铺汇总组装器
 *
 * <p>从 {@code EcMonthlySettlementServiceImpl} 中提取的纯结算计算逻辑：订单归属决策
 * （排除/待定/计入）、店铺汇总金额聚合、最大利润单与待定单 VO 组装。排除买家集合、
 * 订单决策表、行明细、订单列表等数据由服务层加载后作为参数传入，便于独立单元测试，
 * 也让结算服务主类只保留数据加载与持久化编排。无状态，由 Spring 以单例注入。</p>
 */
@Component
public class EcSettlementSummaryAssembler {

    private static final Set<String> AUTO_EXCLUDE_STATUS = Set.of("REFUNDED", "CANCELLED");
    private static final Set<String> PENDING_STATUS = Set.of("DRAFT", "PAID", "PARTIAL_SHIPPED", "PARTIAL_REFUND");
    private static final Set<String> AUTO_INCLUDE_STATUS = Set.of("SHIPPED", "COMPLETED");
    private static final String LINE_RETURNED = "RETURNED";

    /** 店铺汇总：按订单逐条决策聚合营收/成本/利润，并收集待定单与最大利润单 */
    public ShopSummary buildShopSummary(EcShop shop, String shopName, Set<String> excludedBuyers,
                                        Map<Long, EcSettlementOrderDecision> decisionMap,
                                        Map<Long, List<EcSalesOrderLine>> lineMap,
                                        List<EcSalesOrder> orders, boolean expressBillImported) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal estimatedTotalCost = BigDecimal.ZERO;
        BigDecimal actualTotalCost = BigDecimal.ZERO;
        BigDecimal totalActualFreight = BigDecimal.ZERO;
        int includedCount = 0;
        int excludedCount = 0;
        List<PendingOrder> pendingOrders = new ArrayList<>();
        MaxProfitOrder maxProfit = null;

        for (EcSalesOrder order : orders) {
            if (isBuyerExcluded(order.getBuyerName(), excludedBuyers)) {
                excludedCount++;
                continue;
            }
            List<EcSalesOrderLine> lines = lineMap.getOrDefault(order.getId(), List.of());
            SettlementAction action = resolveAction(order, lines, decisionMap.get(order.getId()));
            switch (action.type()) {
                case EXCLUDE -> excludedCount++;
                case PENDING -> pendingOrders.add(toPendingOrder(order, lines, decisionMap.get(order.getId())));
                case INCLUDE_LOSS -> {
                    includedCount++;
                    BigDecimal cost = orderCost(order);
                    BigDecimal estFreight = nvl(order.getEstimatedFreightAmount());
                    BigDecimal actFreight = nvl(order.getActualFreightAmount());
                    estimatedTotalCost = estimatedTotalCost.add(cost).add(estFreight);
                    actualTotalCost = actualTotalCost.add(cost).add(actFreight);
                    totalActualFreight = totalActualFreight.add(actFreight);
                }
                case INCLUDE_PROFIT -> {
                    includedCount++;
                    BigDecimal received = nvl(order.getReceivedAmount());
                    BigDecimal cost = orderCost(order);
                    BigDecimal estFreight = nvl(order.getEstimatedFreightAmount());
                    BigDecimal actFreight = nvl(order.getActualFreightAmount());
                    totalRevenue = totalRevenue.add(received);
                    estimatedTotalCost = estimatedTotalCost.add(cost).add(estFreight);
                    actualTotalCost = actualTotalCost.add(cost).add(actFreight);
                    totalActualFreight = totalActualFreight.add(actFreight);
                    BigDecimal estProfit = received.subtract(cost).subtract(estFreight);
                    if (maxProfit == null || estProfit.compareTo(nvl(maxProfit.getProfitAmount())) > 0) {
                        maxProfit = buildMaxProfitOrder(order, lines, received, cost, estFreight, actFreight, estProfit,
                                expressBillImported);
                    }
                }
            }
        }

        BigDecimal estimatedTotalProfit = totalRevenue.subtract(estimatedTotalCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualTotalProfit = totalRevenue.subtract(actualTotalCost).setScale(2, RoundingMode.HALF_UP);

        ShopSummary summary = new ShopSummary();
        summary.setShopId(shop.getId());
        summary.setShopName(shopName);
        summary.setTotalRevenue(scale2(totalRevenue));
        summary.setEstimatedTotalCost(scale2(estimatedTotalCost));
        summary.setActualTotalCost(scale2(actualTotalCost));
        summary.setEstimatedTotalProfit(estimatedTotalProfit);
        summary.setActualTotalProfit(actualTotalProfit);
        summary.setTotalActualFreight(scale2(totalActualFreight));
        summary.setIncludedOrderCount(includedCount);
        summary.setExcludedOrderCount(excludedCount);
        summary.setPendingOrderCount(pendingOrders.size());
        summary.setMaxProfitOrder(maxProfit);
        summary.setPendingOrders(pendingOrders);
        return summary;
    }

    /** 订单归属决策：退货行优先计入亏损，否则按状态机 + 人工决策判定计入/排除/待定 */
    public SettlementAction resolveAction(EcSalesOrder order, List<EcSalesOrderLine> lines,
                                          EcSettlementOrderDecision decision) {
        if (hasReturnedLine(lines)) {
            return SettlementAction.includeLoss();
        }
        String status = normalizeStatus(order.getStatus());
        if (AUTO_EXCLUDE_STATUS.contains(status)) {
            return SettlementAction.exclude();
        }
        if (AUTO_INCLUDE_STATUS.contains(status)) {
            return SettlementAction.includeProfit();
        }
        if (PENDING_STATUS.contains(status)) {
            if (decision != null) {
                if (Objects.equals(decision.getIncluded(), 1)) {
                    return SettlementAction.includeProfit();
                }
                return SettlementAction.exclude();
            }
            return SettlementAction.pending();
        }
        if (decision != null && Objects.equals(decision.getIncluded(), 1)) {
            return SettlementAction.includeProfit();
        }
        return SettlementAction.exclude();
    }

    private boolean hasReturnedLine(List<EcSalesOrderLine> lines) {
        return lines.stream().anyMatch(l -> LINE_RETURNED.equals(normalizeStatus(l.getStatus())));
    }

    /** 最大利润单：按预估利润取最大，实际利润在快递账单未导入/实际运费缺失时置空并给原因 */
    public MaxProfitOrder buildMaxProfitOrder(EcSalesOrder order, List<EcSalesOrderLine> lines,
                                              BigDecimal received, BigDecimal cost,
                                              BigDecimal estFreight, BigDecimal actFreight,
                                              BigDecimal estProfit, boolean expressBillImported) {
        BigDecimal actProfit = received.subtract(cost).subtract(actFreight);
        MaxProfitOrder maxProfit = new MaxProfitOrder();
        maxProfit.setOrderId(order.getId());
        maxProfit.setOrderNo(order.getOrderNo());
        maxProfit.setPlatformOrderNo(order.getPlatformOrderNo());
        maxProfit.setProductName(joinPendingLineText(lines, false));
        maxProfit.setSkuName(joinPendingLineText(lines, true));
        maxProfit.setOrderTime(order.getOrderTime());
        maxProfit.setReceivedAmount(scale2(received));
        maxProfit.setEstimatedCostAmount(scale2(cost.add(estFreight)));
        maxProfit.setActualCostAmount(scale2(cost.add(actFreight)));
        maxProfit.setProfitAmount(scale2(estProfit));
        if (!expressBillImported) {
            maxProfit.setActualProfitAmount(null);
            maxProfit.setActualProfitUnknownReason("EXPRESS_BILL_NOT_IMPORTED");
        } else if (order.getActualFreightAmount() == null) {
            maxProfit.setActualProfitAmount(null);
            maxProfit.setActualProfitUnknownReason("ACTUAL_FREIGHT_MISSING");
        } else {
            maxProfit.setActualProfitAmount(scale2(actProfit));
        }
        return maxProfit;
    }

    /** 待定单 VO：含订单摘要与人工决策态（未决策时 decided=false） */
    public PendingOrder toPendingOrder(EcSalesOrder order, List<EcSalesOrderLine> lines,
                                       EcSettlementOrderDecision decision) {
        PendingOrder row = new PendingOrder();
        row.setOrderId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setPlatformOrderNo(order.getPlatformOrderNo());
        row.setStatus(order.getStatus());
        row.setBuyerName(order.getBuyerName());
        row.setProductName(joinPendingLineText(lines, false));
        row.setSkuName(joinPendingLineText(lines, true));
        row.setReceivedAmount(order.getReceivedAmount());
        row.setOrderTime(order.getOrderTime());
        if (decision != null) {
            row.setDecided(true);
            row.setIncluded(Objects.equals(decision.getIncluded(), 1));
        } else {
            row.setDecided(false);
            row.setIncluded(null);
        }
        return row;
    }

    private String joinPendingLineText(List<EcSalesOrderLine> lines, boolean sku) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (EcSalesOrderLine line : lines) {
            String text;
            if (sku) {
                text = trimToNull(line.getSkuSpecName());
            } else {
                text = firstNonBlank(line.getLinkName(), line.getPlatformItemName());
            }
            if (text != null) {
                parts.add(text);
            }
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            String text = trimToNull(candidate);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private BigDecimal orderCost(EcSalesOrder order) {
        return nvl(order.getTotalCostAmount());
    }

    private boolean isBuyerExcluded(String buyerName, Set<String> excluded) {
        if (!StringUtils.hasText(buyerName) || excluded.isEmpty()) {
            return false;
        }
        return excluded.contains(buyerName.trim());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale2(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP);
    }

    public record SettlementAction(SettlementType type) {
        static SettlementAction exclude() {
            return new SettlementAction(SettlementType.EXCLUDE);
        }

        static SettlementAction pending() {
            return new SettlementAction(SettlementType.PENDING);
        }

        static SettlementAction includeProfit() {
            return new SettlementAction(SettlementType.INCLUDE_PROFIT);
        }

        static SettlementAction includeLoss() {
            return new SettlementAction(SettlementType.INCLUDE_LOSS);
        }
    }

    public enum SettlementType {
        EXCLUDE, PENDING, INCLUDE_PROFIT, INCLUDE_LOSS
    }
}
