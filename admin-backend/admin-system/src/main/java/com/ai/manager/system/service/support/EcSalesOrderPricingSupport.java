package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.domain.entity.EcListingLinkSku;
import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSalesOrderLine;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.enums.EcListingLinkPricingRisk;
import com.ai.manager.system.domain.vo.EcListingLinkCostBreakdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EcSalesOrderPricingSupport {

    private final EcListingLinkSkuSupport listingLinkSkuSupport;
    private final EcExpressFeeSupport expressFeeSupport;

    public void applyLinePricing(EcSalesOrderLine line,
                                 EcListingLinkSku linkSku,
                                 EcShop shop,
                                 String province,
                                 Long expressStationId) {
        if (line == null || linkSku == null || shop == null) {
            return;
        }
        String skuCodes = linkSku.getSkuCodes();
        EcListingLinkCostBreakdown breakdown = listingLinkSkuSupport.calculateCostBreakdown(
                skuCodes, shop, province, expressStationId);
        BigDecimal discountPct = linkSku.getDiscountPct() != null ? linkSku.getDiscountPct() : new BigDecimal("100");
        BigDecimal coupon = linkSku.getCouponAmount() != null ? linkSku.getCouponAmount() : BigDecimal.ZERO;
        BigDecimal minSet = listingLinkSkuSupport.calculateMinSetAmount(breakdown.getCostPrice(), discountPct, coupon);

        BigDecimal received = line.getLineReceivedAmount();
        if (received == null && line.getUnitPrice() != null && line.getSkuQuantity() != null) {
            received = line.getUnitPrice().multiply(new BigDecimal(line.getSkuQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            line.setLineReceivedAmount(received);
        }
        if (line.getUnitPrice() == null && received != null && line.getSkuQuantity() != null && line.getSkuQuantity() > 0) {
            line.setUnitPrice(received.divide(new BigDecimal(line.getSkuQuantity()), 2, RoundingMode.HALF_UP));
        }

        BigDecimal profit = listingLinkSkuSupport.calculateProfit(
                received, breakdown.getBaseCostAmount(), shop, discountPct, coupon);
        EcListingLinkPricingRisk risk = listingLinkSkuSupport.resolvePricingRisk(received, minSet, profit);

        line.setSkuCodes(skuCodes);
        line.setDiscountPct(discountPct);
        line.setLineCouponAmount(coupon);
        line.setSkuAmount(breakdown.getSkuAmount());
        line.setCartonAmount(breakdown.getCartonAmount());
        line.setExpressAmount(breakdown.getExpressAmount());
        line.setBaseCostAmount(breakdown.getBaseCostAmount());
        line.setPlatformFeeAmount(breakdown.getPlatformFeeAmount());
        line.setCostPrice(breakdown.getCostPrice());
        line.setMinSetAmount(minSet);
        line.setProfit(profit);
        line.setPricingRisk(risk != null ? risk.name() : null);
    }

    public BigDecimal lineTotalCost(EcSalesOrderLine line) {
        if (line == null || line.getCostPrice() == null || line.getSkuQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return line.getCostPrice().multiply(new BigDecimal(line.getSkuQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateLineLoss(EcSalesOrderLine line, BigDecimal allocatedFreight) {
        BigDecimal totalCost = lineTotalCost(line);
        if (allocatedFreight != null) {
            totalCost = totalCost.add(allocatedFreight);
        }
        BigDecimal retained = BigDecimal.ZERO;
        if (line.getLineReceivedAmount() != null && line.getRefundAmount() != null) {
            retained = line.getLineReceivedAmount().subtract(line.getRefundAmount()).max(BigDecimal.ZERO);
        }
        return totalCost.subtract(retained).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateOrderEstimatedFreight(EcSalesOrder order, EcShop shop, List<EcSalesOrderLine> lines) {
        if (order == null || order.getExpressStationId() == null || lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String province = EcAddressProvinceSupport.resolveFreightProvince(
                order.getReceiveProvince(), shop != null ? shop.getDefaultReceiveProvince() : null);
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (EcSalesOrderLine line : lines) {
            if (line == null || "CANCELLED".equals(line.getStatus()) || !StringUtils.hasText(line.getSkuCodes())) {
                continue;
            }
            totalWeight = totalWeight.add(listingLinkSkuSupport.calculateLineShipWeightKg(
                    line.getSkuCodes(), line.getSkuQuantity()));
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            totalWeight = new BigDecimal("0.3");
        }
        try {
            return expressFeeSupport.calculateExpressFee(order.getExpressStationId(), totalWeight, province);
        } catch (BusinessException ex) {
            return BigDecimal.ZERO;
        }
    }
}
