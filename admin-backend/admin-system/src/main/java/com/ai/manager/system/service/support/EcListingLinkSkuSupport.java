package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.enums.EcListingLinkPricingRisk;
import com.ai.manager.system.domain.vo.EcListingLinkCostBreakdown;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EcListingLinkSkuSupport {

    private final EcSkuMapper ecSkuMapper;
    private final EcCartonMapper ecCartonMapper;
    private final EcExpressFeeSupport ecExpressFeeSupport;
    private final EcShopPlatformFeeSupport ecShopPlatformFeeSupport;

    public List<String> parseSkuCodes(String skuCodes) {
        if (!StringUtils.hasText(skuCodes)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不能为空");
        }
        Set<String> parsed = new LinkedHashSet<>();
        for (String part : skuCodes.split(",")) {
            String code = part.trim();
            if (!code.isEmpty()) {
                parsed.add(code);
            }
        }
        if (parsed.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不能为空");
        }
        return new ArrayList<>(parsed);
    }

    public Map<String, EcSku> loadSkuMap(List<String> skuCodes) {
        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .in(EcSku::getSkuCode, skuCodes));
        Map<String, EcSku> map = skus.stream()
                .collect(Collectors.toMap(EcSku::getSkuCode, s -> s, (a, b) -> a));
        List<String> missing = skuCodes.stream().filter(code -> !map.containsKey(code)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "以下货号不存在：" + String.join(", ", missing));
        }
        return map;
    }

    public EcListingLinkCostBreakdown calculateCostBreakdown(String skuCodes, EcShop shop) {
        return calculateCostBreakdown(skuCodes, shop, null, null);
    }

    public EcListingLinkCostBreakdown calculateCostBreakdown(String skuCodes, EcShop shop,
                                                             String province, Long expressStationId) {
        List<String> codes = parseSkuCodes(skuCodes);
        Map<String, EcSku> skuMap = loadSkuMap(codes);
        String effectiveProvince = EcAddressProvinceSupport.resolveFreightProvince(province, resolveProvince(shop));

        BigDecimal skuAmount = BigDecimal.ZERO;
        BigDecimal cartonAmount = BigDecimal.ZERO;
        BigDecimal shipWeightKg = BigDecimal.ZERO;

        for (String code : codes) {
            EcSku sku = skuMap.get(code);
            if (sku.getSalePrice() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "货号 " + code + " 未设置售价，无法计算成本");
            }
            skuAmount = skuAmount.add(sku.getSalePrice());
            cartonAmount = cartonAmount.add(resolveCartonPrice(sku, code));
            shipWeightKg = shipWeightKg.add(resolveShipWeightKg(sku));
        }

        BigDecimal expressAmount = ecExpressFeeSupport.calculateExpressFee(expressStationId, shipWeightKg, effectiveProvince);
        BigDecimal baseCostAmount = skuAmount.add(cartonAmount).add(expressAmount)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal costPrice = ecShopPlatformFeeSupport.calculateBreakEvenCost(baseCostAmount, shop);
        BigDecimal platformFeeAmount = costPrice.subtract(baseCostAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFeePct = ecShopPlatformFeeSupport.sumFeePct(shop);
        BigDecimal fixedPlatformFee = ecShopPlatformFeeSupport.fixedFeePerOrder(shop);

        EcListingLinkCostBreakdown breakdown = new EcListingLinkCostBreakdown();
        breakdown.setSkuAmount(skuAmount.setScale(2, RoundingMode.HALF_UP));
        breakdown.setCartonAmount(cartonAmount.setScale(2, RoundingMode.HALF_UP));
        breakdown.setExpressAmount(expressAmount);
        breakdown.setBaseCostAmount(baseCostAmount);
        breakdown.setPlatformFeeAmount(platformFeeAmount);
        breakdown.setCostPrice(costPrice);
        breakdown.setPlatformFeePct(platformFeePct);
        breakdown.setFixedPlatformFee(fixedPlatformFee);
        breakdown.setShipWeightKg(shipWeightKg.setScale(3, RoundingMode.HALF_UP));
        breakdown.setProvinceName(effectiveProvince);
        breakdown.setCostFormula(buildCostFormula(
                breakdown.getSkuAmount(),
                breakdown.getCartonAmount(),
                breakdown.getExpressAmount(),
                platformFeePct,
                fixedPlatformFee));
        return breakdown;
    }

    public BigDecimal calculateLineShipWeightKg(String skuCodes, Integer skuQuantity) {
        if (!StringUtils.hasText(skuCodes)) {
            return BigDecimal.ZERO;
        }
        int qty = skuQuantity != null && skuQuantity > 0 ? skuQuantity : 1;
        List<String> codes = parseSkuCodes(skuCodes);
        Map<String, EcSku> skuMap = loadSkuMap(codes);
        BigDecimal unitWeight = BigDecimal.ZERO;
        for (String code : codes) {
            unitWeight = unitWeight.add(resolveShipWeightKg(skuMap.get(code)));
        }
        return unitWeight.multiply(new BigDecimal(qty)).setScale(3, RoundingMode.HALF_UP);
    }

    public String buildCostFormula(BigDecimal skuAmount,
                                   BigDecimal cartonAmount,
                                   BigDecimal expressAmount,
                                   BigDecimal platformFeePct,
                                   BigDecimal fixedPlatformFee) {
        return String.format(
                "成本 = (SKU售价 %s + 纸箱 %s + 快递 %s + 固定平台费 %s) ÷ (1 - 平台费率 %s%%)",
                formatMoney(skuAmount),
                formatMoney(cartonAmount),
                formatMoney(expressAmount),
                formatMoney(fixedPlatformFee),
                platformFeePct != null ? platformFeePct.stripTrailingZeros().toPlainString() : "0");
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String resolveProvince(EcShop shop) {
        if (shop != null && StringUtils.hasText(shop.getDefaultReceiveProvince())) {
            return shop.getDefaultReceiveProvince().trim();
        }
        return EcExpressFeeSupport.DEFAULT_PROVINCE;
    }

    private BigDecimal resolveCartonPrice(EcSku sku, String skuCode) {
        if (sku.getCartonId() == null) {
            return BigDecimal.ZERO;
        }
        EcCarton carton = ecCartonMapper.selectById(sku.getCartonId());
        if (carton == null || carton.getUnitPrice() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "货号 " + skuCode + " 关联纸箱未设置单价，无法计算成本");
        }
        return carton.getUnitPrice();
    }

    private BigDecimal resolveShipWeightKg(EcSku sku) {
        if (sku.getCartonGrossWeightKg() == null) {
            return new BigDecimal("0.3");
        }
        if (sku.getUnitsPerCarton() != null && sku.getUnitsPerCarton() > 1) {
            return sku.getCartonGrossWeightKg()
                    .divide(new BigDecimal(sku.getUnitsPerCarton()), 3, RoundingMode.HALF_UP);
        }
        return sku.getCartonGrossWeightKg();
    }

    public BigDecimal resolveDiscountMultiplier(BigDecimal discountPct) {
        BigDecimal discount = discountPct != null ? discountPct : new BigDecimal("100");
        if (discount.compareTo(BigDecimal.ZERO) <= 0 || discount.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "折扣须在 0~100 之间（如 90 表示 9 折）");
        }
        return discount.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetRevenue(BigDecimal setAmount,
                                          BigDecimal discountPct,
                                          BigDecimal couponAmount) {
        if (setAmount == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "设置金额不能为空");
        }
        BigDecimal coupon = couponAmount != null ? couponAmount : BigDecimal.ZERO;
        if (coupon.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "优惠券金额不能为负");
        }
        if (setAmount.compareTo(coupon) < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "设置金额不能小于优惠券");
        }
        BigDecimal multiplier = resolveDiscountMultiplier(discountPct);
        return setAmount.subtract(coupon).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateMinSetAmount(BigDecimal costPrice,
                                            BigDecimal discountPct,
                                            BigDecimal couponAmount) {
        if (costPrice == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "成本价格不能为空");
        }
        BigDecimal coupon = couponAmount != null ? couponAmount : BigDecimal.ZERO;
        BigDecimal multiplier = resolveDiscountMultiplier(discountPct);
        BigDecimal result = costPrice.divide(multiplier, 6, RoundingMode.HALF_UP).add(coupon);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            result = BigDecimal.ZERO;
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateProfit(BigDecimal actualSetAmount,
                                      BigDecimal baseCostAmount,
                                      EcShop shop,
                                      BigDecimal discountPct,
                                      BigDecimal couponAmount) {
        if (actualSetAmount == null || baseCostAmount == null) {
            return null;
        }
        BigDecimal netRevenue = calculateNetRevenue(actualSetAmount, discountPct, couponAmount);
        BigDecimal platformFee = ecShopPlatformFeeSupport.calculatePlatformFee(netRevenue, shop);
        return netRevenue.subtract(baseCostAmount).subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
    }

    public EcListingLinkPricingRisk resolvePricingRisk(BigDecimal actualSetAmount,
                                                       BigDecimal minSetAmount,
                                                       BigDecimal profit) {
        if (actualSetAmount != null && minSetAmount != null
                && actualSetAmount.compareTo(minSetAmount) < 0) {
            return EcListingLinkPricingRisk.BELOW_MIN;
        }
        if (profit != null && profit.compareTo(BigDecimal.ZERO) < 0) {
            return EcListingLinkPricingRisk.NEGATIVE_PROFIT;
        }
        return EcListingLinkPricingRisk.OK;
    }

    public String normalizeSkuCodes(String skuCodes) {
        return String.join(",", parseSkuCodes(skuCodes));
    }
}
