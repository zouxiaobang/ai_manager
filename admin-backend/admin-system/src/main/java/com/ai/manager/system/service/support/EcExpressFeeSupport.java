package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.EcExpressPrice;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.mapper.EcExpressPriceMapper;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EcExpressFeeSupport {

    public static final String DEFAULT_PROVINCE = "广东省";

    private final EcExpressStationMapper ecExpressStationMapper;
    private final EcExpressPriceMapper ecExpressPriceMapper;

    public EcExpressStation requireDefaultStation() {
        EcExpressStation station = ecExpressStationMapper.selectOne(new LambdaQueryWrapper<EcExpressStation>()
                .eq(EcExpressStation::getIsDefault, 1)
                .last("LIMIT 1"));
        if (station == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未配置默认快递站点，无法计算快递费");
        }
        return station;
    }

    public BigDecimal calculateExpressFee(BigDecimal weightKg, String provinceName) {
        EcExpressStation station = requireDefaultStation();
        return calculateExpressFee(station.getId(), weightKg, provinceName);
    }

    public BigDecimal calculateExpressFee(Long stationId, BigDecimal weightKg, String provinceName) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            weightKg = new BigDecimal("0.3");
        }
        if (stationId == null) {
            EcExpressStation station = requireDefaultStation();
            stationId = station.getId();
        }
        EcExpressStation station = ecExpressStationMapper.selectById(stationId);
        if (station == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "快递站点不存在");
        }
        String province = EcAddressProvinceSupport.normalizeProvinceName(provinceName);
        if (!StringUtils.hasText(province)) {
            province = DEFAULT_PROVINCE;
        }
        EcExpressPrice price = findPriceForProvince(station.getId(), province);
        if (price == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "快递站点「" + station.getName() + "」未配置「" + province + "」价格");
        }
        return resolvePriceByWeight(weightKg, price).setScale(2, RoundingMode.HALF_UP);
    }

    private EcExpressPrice findPriceForProvince(Long stationId, String provinceName) {
        List<EcExpressPrice> prices = ecExpressPriceMapper.selectList(new LambdaQueryWrapper<EcExpressPrice>()
                .eq(EcExpressPrice::getStationId, stationId));
        EcExpressPrice matched = matchProvincePrice(prices, provinceName);
        if (matched != null) {
            return matched;
        }
        if (!DEFAULT_PROVINCE.equals(provinceName)) {
            return matchProvincePrice(prices, DEFAULT_PROVINCE);
        }
        return null;
    }

    private EcExpressPrice matchProvincePrice(List<EcExpressPrice> prices, String targetProvince) {
        if (!StringUtils.hasText(targetProvince) || prices == null || prices.isEmpty()) {
            return null;
        }
        for (EcExpressPrice price : prices) {
            if (EcAddressProvinceSupport.provinceNamesEquivalent(targetProvince, price.getProvinceName())) {
                return price;
            }
        }
        return null;
    }

    private BigDecimal resolvePriceByWeight(BigDecimal weightKg, EcExpressPrice price) {
        BigDecimal weight = weightKg.setScale(3, RoundingMode.HALF_UP);
        if (weight.compareTo(new BigDecimal("0.3")) <= 0) {
            return requirePrice(price.getPriceW03Kg(), "0.3kg");
        }
        if (weight.compareTo(new BigDecimal("0.5")) <= 0) {
            return requirePrice(price.getPriceW05Kg(), "0.5kg");
        }
        if (weight.compareTo(BigDecimal.ONE) <= 0) {
            return requirePrice(price.getPriceW1Kg(), "1kg");
        }
        if (weight.compareTo(new BigDecimal("1.5")) <= 0) {
            return requirePrice(price.getPriceW15Kg(), "1.5kg");
        }
        if (weight.compareTo(new BigDecimal("2")) <= 0) {
            return requirePrice(price.getPriceW2Kg(), "2kg");
        }
        if (weight.compareTo(new BigDecimal("2.5")) <= 0) {
            return requirePrice(price.getPriceW25Kg(), "2.5kg");
        }
        if (weight.compareTo(new BigDecimal("3")) <= 0) {
            return requirePrice(price.getPriceW3Kg(), "3kg");
        }
        BigDecimal over3First = requirePrice(price.getOver3FirstPrice(), "超3kg首重");
        BigDecimal over3Additional = requirePrice(price.getOver3AdditionalPrice(), "超3kg续重");
        BigDecimal billingWeight = weight.setScale(0, RoundingMode.CEILING);
        BigDecimal extraKg = billingWeight.subtract(new BigDecimal("3"));
        if (extraKg.compareTo(BigDecimal.ZERO) <= 0) {
            extraKg = BigDecimal.ONE;
        }
        return over3First.add(over3Additional.multiply(extraKg));
    }

    private BigDecimal requirePrice(BigDecimal price, String tierLabel) {
        if (price == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "快递价格表缺少「" + tierLabel + "」档位");
        }
        return price;
    }
}
