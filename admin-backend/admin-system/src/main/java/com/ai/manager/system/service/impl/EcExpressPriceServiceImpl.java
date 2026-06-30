package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcExpressPriceSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressPrice;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.vo.EcExpressPriceVO;
import com.ai.manager.system.mapper.EcExpressPriceMapper;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.ai.manager.system.service.EcExpressPriceService;
import com.ai.manager.system.service.support.EcAddressProvinceSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EcExpressPriceServiceImpl extends ServiceImpl<EcExpressPriceMapper, EcExpressPrice>
        implements EcExpressPriceService {

    private final EcExpressStationMapper ecExpressStationMapper;

    @Override
    public List<EcExpressPriceVO> listPrices(Long stationId) {
        requireStation(stationId);
        List<EcExpressPrice> prices = list(new LambdaQueryWrapper<EcExpressPrice>()
                .eq(EcExpressPrice::getStationId, stationId)
                .orderByAsc(EcExpressPrice::getProvinceName));
        List<EcExpressPriceVO> result = new ArrayList<>();
        for (EcExpressPrice price : prices) {
            result.add(toVO(price));
        }
        return result;
    }

    @Override
    public List<String> listRegionNames() {
        return list(new LambdaQueryWrapper<EcExpressPrice>()
                .select(EcExpressPrice::getProvinceName)
                .orderByAsc(EcExpressPrice::getProvinceName))
                .stream()
                .map(EcExpressPrice::getProvinceName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcExpressPriceVO createPrice(EcExpressPriceSaveRequest request) {
        validateRequest(request, true);
        requireStation(request.getStationId());
        EcExpressPrice price = applyFields(request, new EcExpressPrice());
        try {
            save(price);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该省份价格已存在");
        }
        return toVO(price);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcExpressPriceVO updatePrice(Long id, EcExpressPriceSaveRequest request) {
        EcExpressPrice existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateRequest(request, false);
        if (request.getStationId() != null && !request.getStationId().equals(existing.getStationId())) {
            requireStation(request.getStationId());
            existing.setStationId(request.getStationId());
        }
        applyFields(request, existing);
        try {
            updateById(existing);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该省份价格已存在");
        }
        return toVO(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePrice(Long id) {
        EcExpressPrice existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        removeById(id);
    }

    private void validateRequest(EcExpressPriceSaveRequest request, boolean creating) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        if (creating && request.getStationId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "站点不能为空");
        }
        if (!StringUtils.hasText(request.getProvinceName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "省份名称不能为空");
        }
        String provinceName = EcAddressProvinceSupport.normalizeProvinceName(request.getProvinceName());
        if (!StringUtils.hasText(provinceName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "省份名称无法识别，请填写如「广东」或「广东省」");
        }
        request.setProvinceName(provinceName);
        validateNonNegative(request.getPriceW03Kg(), "0.3kg价格");
        validateNonNegative(request.getPriceW05Kg(), "0.5kg价格");
        validateNonNegative(request.getPriceW1Kg(), "1kg价格");
        validateNonNegative(request.getPriceW15Kg(), "1.5kg价格");
        validateNonNegative(request.getPriceW2Kg(), "2kg价格");
        validateNonNegative(request.getPriceW25Kg(), "2.5kg价格");
        validateNonNegative(request.getPriceW3Kg(), "3kg价格");
        validateNonNegative(request.getOver3FirstPrice(), "超3kg首重价格");
        validateNonNegative(request.getOver3AdditionalPrice(), "超3kg续重价格");
    }

    private void validateNonNegative(BigDecimal value, String label) {
        if (value != null && value.signum() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), label + "不能为负数");
        }
    }

    private EcExpressPrice applyFields(EcExpressPriceSaveRequest request, EcExpressPrice price) {
        if (request.getStationId() != null) {
            price.setStationId(request.getStationId());
        }
        price.setProvinceName(request.getProvinceName().trim());
        price.setPriceW03Kg(request.getPriceW03Kg());
        price.setPriceW05Kg(request.getPriceW05Kg());
        price.setPriceW1Kg(request.getPriceW1Kg());
        price.setPriceW15Kg(request.getPriceW15Kg());
        price.setPriceW2Kg(request.getPriceW2Kg());
        price.setPriceW25Kg(request.getPriceW25Kg());
        price.setPriceW3Kg(request.getPriceW3Kg());
        price.setOver3FirstPrice(request.getOver3FirstPrice());
        price.setOver3AdditionalPrice(request.getOver3AdditionalPrice());
        return price;
    }

    private void requireStation(Long stationId) {
        EcExpressStation station = ecExpressStationMapper.selectById(stationId);
        if (station == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "快递站点不存在");
        }
    }

    private EcExpressPriceVO toVO(EcExpressPrice price) {
        EcExpressPriceVO vo = new EcExpressPriceVO();
        vo.setId(price.getId());
        vo.setStationId(price.getStationId());
        vo.setProvinceName(price.getProvinceName());
        vo.setPriceW03Kg(price.getPriceW03Kg());
        vo.setPriceW05Kg(price.getPriceW05Kg());
        vo.setPriceW1Kg(price.getPriceW1Kg());
        vo.setPriceW15Kg(price.getPriceW15Kg());
        vo.setPriceW2Kg(price.getPriceW2Kg());
        vo.setPriceW25Kg(price.getPriceW25Kg());
        vo.setPriceW3Kg(price.getPriceW3Kg());
        vo.setOver3FirstPrice(price.getOver3FirstPrice());
        vo.setOver3AdditionalPrice(price.getOver3AdditionalPrice());
        vo.setUpdateTime(price.getUpdateTime());
        return vo;
    }
}
