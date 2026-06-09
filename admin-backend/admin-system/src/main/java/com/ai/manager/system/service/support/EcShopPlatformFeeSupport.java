package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.EcShop;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EcShopPlatformFeeSupport {

    public BigDecimal sumFeePct(EcShop shop) {
        if (shop == null) {
            return BigDecimal.ZERO;
        }
        return sum(
                shop.getCategoryCommissionPct(),
                shop.getTechServiceFeePct(),
                shop.getPaymentFeePct(),
                shop.getPromotionFeePct(),
                shop.getFulfillmentFeePct(),
                shop.getReturnServiceFeePct(),
                shop.getInstallmentFeePct(),
                shop.getActivityServiceFeePct(),
                shop.getOtherFeePct());
    }

    public BigDecimal fixedFeePerOrder(EcShop shop) {
        if (shop == null || shop.getShippingInsuranceFee() == null) {
            return BigDecimal.ZERO;
        }
        return shop.getShippingInsuranceFee();
    }

    /**
     * 含平台费的盈亏平衡成本。
     * 净收入 R 满足：R = 基础成本 + R×费率 + 固定费  =>  R = (基础成本 + 固定费) / (1 - 费率)
     */
    public BigDecimal calculateBreakEvenCost(BigDecimal baseCost, EcShop shop) {
        if (baseCost == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "基础成本不能为空");
        }
        BigDecimal fixed = fixedFeePerOrder(shop);
        BigDecimal feePct = sumFeePct(shop);
        if (feePct.compareTo(new BigDecimal("100")) >= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "店铺平台费率合计不能大于等于 100%");
        }
        BigDecimal divisor = BigDecimal.ONE.subtract(
                feePct.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return baseCost.add(fixed).divide(divisor, 6, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** 按给定净收入计算平台扣费 */
    public BigDecimal calculatePlatformFee(BigDecimal netRevenue, EcShop shop) {
        if (netRevenue == null || netRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return fixedFeePerOrder(shop).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal pctFee = netRevenue.multiply(sumFeePct(shop))
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        return pctFee.add(fixedFeePerOrder(shop)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        if (values == null) {
            return total;
        }
        for (BigDecimal value : values) {
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }
}
