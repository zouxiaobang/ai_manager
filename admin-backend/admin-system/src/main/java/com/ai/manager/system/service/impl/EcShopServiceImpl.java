package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcShopSaveRequest;
import com.ai.manager.system.domain.entity.EcListingLink;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.vo.EcShopListItemVO;
import com.ai.manager.system.mapper.EcListingLinkMapper;
import com.ai.manager.system.mapper.EcPlatformMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.service.EcShopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EcShopServiceImpl extends ServiceImpl<EcShopMapper, EcShop> implements EcShopService {

    private final EcPlatformMapper ecPlatformMapper;
    private final EcListingLinkMapper ecListingLinkMapper;

    @Override
    public PageResult<EcShopListItemVO> pageShops(String keyword, Long platformId, Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcShop> wrapper = new LambdaQueryWrapper<EcShop>()
                .orderByDesc(EcShop::getId);
        if (platformId != null) {
            wrapper.eq(EcShop::getPlatformId, platformId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcShop::getName, kw)
                    .or().like(EcShop::getNameEn, kw)
                    .or().like(EcShop::getRemark, kw));
        }
        Page<EcShop> entityPage = page(new Page<>(p, ps), wrapper);
        Map<Long, EcPlatform> platformMap = loadPlatformMap(entityPage.getRecords().stream()
                .map(EcShop::getPlatformId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        List<EcShopListItemVO> records = new ArrayList<>();
        for (EcShop shop : entityPage.getRecords()) {
            records.add(toListItemVO(shop, platformMap.get(shop.getPlatformId())));
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public List<EcShopListItemVO> listShopOptions(Long platformId) {
        LambdaQueryWrapper<EcShop> wrapper = new LambdaQueryWrapper<EcShop>()
                .eq(EcShop::getStatus, "ENABLED")
                .orderByAsc(EcShop::getName);
        if (platformId != null) {
            wrapper.eq(EcShop::getPlatformId, platformId);
        }
        List<EcShop> shops = list(wrapper);
        Map<Long, EcPlatform> platformMap = loadPlatformMap(shops.stream()
                .map(EcShop::getPlatformId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        return shops.stream()
                .map(shop -> toListItemVO(shop, platformMap.get(shop.getPlatformId())))
                .toList();
    }

    @Override
    public EcShopListItemVO getShopDetail(Long id) {
        EcShop shop = getById(id);
        if (shop == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        EcPlatform platform = shop.getPlatformId() == null ? null : ecPlatformMapper.selectById(shop.getPlatformId());
        return toListItemVO(shop, platform);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcShopListItemVO createShop(EcShopSaveRequest request) {
        validateRequest(request);
        EcShop shop = applySaveFields(request, new EcShop());
        save(shop);
        return getShopDetail(shop.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcShopListItemVO updateShop(Long id, EcShopSaveRequest request) {
        EcShop existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateRequest(request);
        applySaveFields(request, existing);
        updateById(existing);
        return getShopDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShop(Long id) {
        EcShop existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long linkCount = ecListingLinkMapper.selectCount(new LambdaQueryWrapper<EcListingLink>()
                .eq(EcListingLink::getShopId, id));
        if (linkCount != null && linkCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该店铺下仍有关联链接，无法删除");
        }
        removeById(id);
    }

    private void validateRequest(EcShopSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "店铺名称不能为空");
        }
        if (request.getPlatformId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择所属平台");
        }
        EcPlatform platform = ecPlatformMapper.selectById(request.getPlatformId());
        if (platform == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "所属平台不存在");
        }
    }

    private EcShop applySaveFields(EcShopSaveRequest request, EcShop shop) {
        shop.setName(request.getName().trim());
        shop.setNameEn(trimToNull(request.getNameEn()));
        shop.setPlatformId(request.getPlatformId());
        shop.setRemark(trimToNull(request.getRemark()));
        shop.setCategoryCommissionPct(request.getCategoryCommissionPct());
        shop.setTechServiceFeePct(request.getTechServiceFeePct());
        shop.setPaymentFeePct(request.getPaymentFeePct());
        shop.setPromotionFeePct(request.getPromotionFeePct());
        shop.setFulfillmentFeePct(request.getFulfillmentFeePct());
        shop.setReturnServiceFeePct(request.getReturnServiceFeePct());
        shop.setInstallmentFeePct(request.getInstallmentFeePct());
        shop.setActivityServiceFeePct(request.getActivityServiceFeePct());
        shop.setAnnualPlatformFee(request.getAnnualPlatformFee());
        shop.setDepositAmount(request.getDepositAmount());
        shop.setShippingInsuranceFee(request.getShippingInsuranceFee());
        shop.setOtherFeePct(request.getOtherFeePct());
        shop.setOtherFeeRemark(trimToNull(request.getOtherFeeRemark()));
        shop.setDefaultReceiveProvince(StringUtils.hasText(request.getDefaultReceiveProvince())
                ? request.getDefaultReceiveProvince().trim() : "广东省");
        if (StringUtils.hasText(request.getStatus())) {
            shop.setStatus(request.getStatus().trim().toUpperCase());
        } else if (shop.getStatus() == null) {
            shop.setStatus("ENABLED");
        }
        return shop;
    }

    private Map<Long, EcPlatform> loadPlatformMap(List<Long> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return Map.of();
        }
        return ecPlatformMapper.selectBatchIds(platformIds).stream()
                .collect(Collectors.toMap(EcPlatform::getId, p -> p, (a, b) -> a));
    }

    private EcShopListItemVO toListItemVO(EcShop shop, EcPlatform platform) {
        EcShopListItemVO vo = new EcShopListItemVO();
        vo.setId(shop.getId());
        vo.setName(shop.getName());
        vo.setNameEn(shop.getNameEn());
        vo.setPlatformId(shop.getPlatformId());
        if (platform != null) {
            vo.setPlatformName(platform.getName());
            vo.setPlatformCode(platform.getPlatformCode());
            vo.setChannelType(platform.getChannelType());
        }
        vo.setRemark(shop.getRemark());
        vo.setCategoryCommissionPct(shop.getCategoryCommissionPct());
        vo.setTechServiceFeePct(shop.getTechServiceFeePct());
        vo.setPaymentFeePct(shop.getPaymentFeePct());
        vo.setPromotionFeePct(shop.getPromotionFeePct());
        vo.setFulfillmentFeePct(shop.getFulfillmentFeePct());
        vo.setReturnServiceFeePct(shop.getReturnServiceFeePct());
        vo.setInstallmentFeePct(shop.getInstallmentFeePct());
        vo.setActivityServiceFeePct(shop.getActivityServiceFeePct());
        vo.setAnnualPlatformFee(shop.getAnnualPlatformFee());
        vo.setDepositAmount(shop.getDepositAmount());
        vo.setShippingInsuranceFee(shop.getShippingInsuranceFee());
        vo.setOtherFeePct(shop.getOtherFeePct());
        vo.setOtherFeeRemark(shop.getOtherFeeRemark());
        vo.setDefaultReceiveProvince(shop.getDefaultReceiveProvince());
        vo.setStatus(shop.getStatus());
        vo.setUpdateTime(shop.getUpdateTime());
        return vo;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
