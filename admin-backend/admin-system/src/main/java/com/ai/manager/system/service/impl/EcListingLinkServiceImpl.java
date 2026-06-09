package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcListingLinkPricingRequest;
import com.ai.manager.system.domain.dto.EcListingLinkSaveRequest;
import com.ai.manager.system.domain.dto.EcListingLinkSkuItem;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcListingLink;
import com.ai.manager.system.domain.entity.EcListingLinkProduct;
import com.ai.manager.system.domain.entity.EcListingLinkSku;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.enums.EcListingLinkPricingRisk;
import com.ai.manager.system.domain.vo.EcListingLinkCostBreakdown;
import com.ai.manager.system.domain.vo.EcListingLinkDetailVO;
import com.ai.manager.system.domain.vo.EcListingLinkPricingVO;
import com.ai.manager.system.domain.vo.EcListingLinkProductVO;
import com.ai.manager.system.domain.vo.EcListingLinkSkuVO;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcListingLinkMapper;
import com.ai.manager.system.mapper.EcListingLinkProductMapper;
import com.ai.manager.system.mapper.EcListingLinkSkuMapper;
import com.ai.manager.system.mapper.EcPlatformMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.service.EcListingLinkService;
import com.ai.manager.system.service.support.EcListingLinkInventorySupport;
import com.ai.manager.system.service.support.EcListingLinkSkuSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcListingLinkServiceImpl extends ServiceImpl<EcListingLinkMapper, EcListingLink>
        implements EcListingLinkService {

    private final EcListingLinkSkuMapper ecListingLinkSkuMapper;
    private final EcListingLinkProductMapper ecListingLinkProductMapper;
    private final EcShopMapper ecShopMapper;
    private final EcPlatformMapper ecPlatformMapper;
    private final EcProductMapper ecProductMapper;
    private final EcSkuMapper ecSkuMapper;
    private final EcFactoryMapper ecFactoryMapper;
    private final EcListingLinkSkuSupport listingLinkSkuSupport;
    private final EcListingLinkInventorySupport listingLinkInventorySupport;

    @Override
    public PageResult<EcListingLinkDetailVO> pageLinks(String keyword, Long shopId, Long platformId,
                                                       Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcListingLink> wrapper = new LambdaQueryWrapper<EcListingLink>()
                .orderByDesc(EcListingLink::getListingTime)
                .orderByDesc(EcListingLink::getId);

        Set<Long> allowedShopIds = resolveShopIds(shopId, platformId);
        if (allowedShopIds != null) {
            if (allowedShopIds.isEmpty()) {
                return PageResult.empty(p, ps);
            }
            wrapper.in(EcListingLink::getShopId, allowedShopIds);
        }

        Set<Long> keywordLinkIds = resolveLinkIdsByKeyword(keyword);
        if (keywordLinkIds != null) {
            if (keywordLinkIds.isEmpty()) {
                return PageResult.empty(p, ps);
            }
            wrapper.in(EcListingLink::getId, keywordLinkIds);
        }

        Page<EcListingLink> entityPage = page(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            return PageUtils.of(List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        }

        Map<Long, EcShop> shopMap = loadShopMap(entityPage.getRecords().stream()
                .map(EcListingLink::getShopId).distinct().toList());
        Map<Long, Long> skuCountMap = loadSkuCountMap(entityPage.getRecords().stream()
                .map(EcListingLink::getId).toList());
        Map<Long, EcPlatform> platformMap = loadPlatformMap(shopMap.values().stream()
                .map(EcShop::getPlatformId).filter(Objects::nonNull).distinct().toList());
        List<Long> linkIds = entityPage.getRecords().stream().map(EcListingLink::getId).toList();
        Map<Long, List<EcListingLinkProductVO>> linkProductsMap = loadLinkProductsMap(linkIds);

        List<EcListingLinkDetailVO> records = new ArrayList<>();
        for (EcListingLink link : entityPage.getRecords()) {
            EcShop shop = shopMap.get(link.getShopId());
            EcPlatform platform = shop != null && shop.getPlatformId() != null
                    ? platformMap.get(shop.getPlatformId()) : null;
            EcListingLinkDetailVO vo = toDetailVO(link, shop, platform,
                    linkProductsMap.getOrDefault(link.getId(), List.of()), List.of());
            vo.setSkuCount(skuCountMap.getOrDefault(link.getId(), 0L).intValue());
            records.add(vo);
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public List<EcListingLinkDetailVO> listLinksByProductId(Long productId) {
        if (productId == null) {
            return List.of();
        }
        List<EcListingLinkProduct> relations = ecListingLinkProductMapper.selectList(
                new LambdaQueryWrapper<EcListingLinkProduct>()
                        .eq(EcListingLinkProduct::getProductId, productId)
                        .orderByAsc(EcListingLinkProduct::getSortOrder));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> linkIds = relations.stream().map(EcListingLinkProduct::getLinkId).distinct().toList();
        List<EcListingLink> links = listByIds(linkIds).stream()
                .sorted((a, b) -> {
                    if (a.getListingTime() == null && b.getListingTime() == null) {
                        return Long.compare(b.getId(), a.getId());
                    }
                    if (a.getListingTime() == null) return 1;
                    if (b.getListingTime() == null) return -1;
                    int cmp = b.getListingTime().compareTo(a.getListingTime());
                    return cmp != 0 ? cmp : Long.compare(b.getId(), a.getId());
                })
                .toList();
        Map<Long, EcShop> shopMap = loadShopMap(links.stream().map(EcListingLink::getShopId).distinct().toList());
        Map<Long, EcPlatform> platformMap = loadPlatformMap(shopMap.values().stream()
                .map(EcShop::getPlatformId).filter(Objects::nonNull).distinct().toList());
        Map<Long, List<EcListingLinkProductVO>> linkProductsMap = loadLinkProductsMap(linkIds);
        List<EcListingLinkDetailVO> result = new ArrayList<>();
        for (EcListingLink link : links) {
            EcShop shop = shopMap.get(link.getShopId());
            EcPlatform platform = shop != null && shop.getPlatformId() != null
                    ? platformMap.get(shop.getPlatformId()) : null;
            result.add(toDetailVO(link, shop, platform,
                    linkProductsMap.getOrDefault(link.getId(), List.of()), List.of()));
        }
        return result;
    }

    @Override
    public EcListingLinkDetailVO getLinkDetail(Long id) {
        EcListingLink link = getById(id);
        if (link == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        EcShop shop = ecShopMapper.selectById(link.getShopId());
        EcPlatform platform = shop != null && shop.getPlatformId() != null
                ? ecPlatformMapper.selectById(shop.getPlatformId()) : null;
        List<EcListingLinkProductVO> products = loadLinkProducts(id);
        List<EcListingLinkSkuVO> skus = loadSkus(id, shop);
        EcListingLinkDetailVO vo = toDetailVO(link, shop, platform, products, skus);
        if (!skus.isEmpty() && skus.get(0).getSkuAmount() != null) {
            vo.setCostFormula(listingLinkSkuSupport.buildCostFormula(
                    skus.get(0).getSkuAmount(),
                    skus.get(0).getCartonAmount(),
                    skus.get(0).getExpressAmount(),
                    skus.get(0).getPlatformFeePct(),
                    shop != null ? shop.getShippingInsuranceFee() : BigDecimal.ZERO));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcListingLinkDetailVO createLink(EcListingLinkSaveRequest request) {
        List<NormalizedSku> skus = validateAndNormalizeSkus(request);
        EcShop shop = requireShop(request.getShopId());
        validateProductIds(request.getProductIds());
        EcListingLink link = new EcListingLink();
        applyLinkFields(link, request, shop);
        save(link);
        saveLinkProducts(link.getId(), request.getProductIds());
        saveSkus(link.getId(), skus);
        return getLinkDetail(link.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcListingLinkDetailVO updateLink(Long id, EcListingLinkSaveRequest request) {
        EcListingLink existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<NormalizedSku> skus = validateAndNormalizeSkus(request);
        EcShop shop = requireShop(request.getShopId());
        validateProductIds(request.getProductIds());
        applyLinkFields(existing, request, shop);
        updateById(existing);
        ecListingLinkProductMapper.delete(new LambdaQueryWrapper<EcListingLinkProduct>()
                .eq(EcListingLinkProduct::getLinkId, id));
        saveLinkProducts(id, request.getProductIds());
        ecListingLinkSkuMapper.delete(new LambdaQueryWrapper<EcListingLinkSku>()
                .eq(EcListingLinkSku::getLinkId, id));
        saveSkus(id, skus);
        return getLinkDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcListingLinkDetailVO copyLink(Long id) {
        EcListingLinkDetailVO source = getLinkDetail(id);
        EcListingLinkSaveRequest request = new EcListingLinkSaveRequest();
        request.setName(source.getName() + " (复制)");
        request.setShopId(source.getShopId());
        request.setPlatformUrl(source.getPlatformUrl());
        request.setProductIds(source.getProducts() == null ? List.of()
                : source.getProducts().stream().map(EcListingLinkProductVO::getProductId).filter(Objects::nonNull).toList());
        request.setListingTime(LocalDateTime.now());
        request.setRemark(source.getRemark());
        request.setStatus(source.getStatus());
        request.setSkus(source.getSkus() == null ? List.of() : source.getSkus().stream().map(s -> {
            EcListingLinkSkuItem item = new EcListingLinkSkuItem();
            item.setSkuName(s.getSkuName());
            item.setSkuCodes(s.getSkuCodes());
            item.setDiscountPct(s.getDiscountPct());
            item.setCouponAmount(s.getCouponAmount());
            item.setActualSetAmount(s.getActualSetAmount());
            item.setSortOrder(s.getSortOrder());
            return item;
        }).toList());
        return createLink(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLink(Long id) {
        EcListingLink existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ecListingLinkSkuMapper.delete(new LambdaQueryWrapper<EcListingLinkSku>()
                .eq(EcListingLinkSku::getLinkId, id));
        ecListingLinkProductMapper.delete(new LambdaQueryWrapper<EcListingLinkProduct>()
                .eq(EcListingLinkProduct::getLinkId, id));
        removeById(id);
    }

    @Override
    public EcListingLinkPricingVO calculatePricing(EcListingLinkPricingRequest request) {
        if (request == null || !StringUtils.hasText(request.getSkuCodes())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不能为空");
        }
        EcShop shop = requireShop(request.getShopId());
        String skuCodes = listingLinkSkuSupport.normalizeSkuCodes(request.getSkuCodes());
        EcListingLinkCostBreakdown breakdown = listingLinkSkuSupport.calculateCostBreakdown(skuCodes, shop);
        BigDecimal minSetAmount = listingLinkSkuSupport.calculateMinSetAmount(
                breakdown.getCostPrice(), request.getDiscountPct(), request.getCouponAmount());
        BigDecimal profit = listingLinkSkuSupport.calculateProfit(
                request.getActualSetAmount(),
                breakdown.getBaseCostAmount(),
                shop,
                request.getDiscountPct(),
                request.getCouponAmount());
        EcListingLinkPricingRisk risk = listingLinkSkuSupport.resolvePricingRisk(
                request.getActualSetAmount(), minSetAmount, profit);

        EcListingLinkPricingVO vo = new EcListingLinkPricingVO();
        vo.setSkuCodes(skuCodes);
        vo.setSkuAmount(breakdown.getSkuAmount());
        vo.setCartonAmount(breakdown.getCartonAmount());
        vo.setExpressAmount(breakdown.getExpressAmount());
        vo.setBaseCostAmount(breakdown.getBaseCostAmount());
        vo.setPlatformFeeAmount(breakdown.getPlatformFeeAmount());
        vo.setPlatformFeePct(breakdown.getPlatformFeePct());
        vo.setFixedPlatformFee(breakdown.getFixedPlatformFee());
        vo.setCostPrice(breakdown.getCostPrice());
        vo.setMinSetAmount(minSetAmount);
        vo.setActualSetAmount(request.getActualSetAmount());
        vo.setProfit(profit);
        vo.setShipWeightKg(breakdown.getShipWeightKg());
        vo.setProvinceName(breakdown.getProvinceName());
        vo.setPricingRisk(risk);
        vo.setCostFormula(breakdown.getCostFormula());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recalculateAllPricing() {
        List<EcListingLink> links = list();
        int updated = 0;
        for (EcListingLink link : links) {
            EcShop shop = ecShopMapper.selectById(link.getShopId());
            if (shop == null) {
                continue;
            }
            List<EcListingLinkSku> skus = ecListingLinkSkuMapper.selectList(
                    new LambdaQueryWrapper<EcListingLinkSku>()
                            .eq(EcListingLinkSku::getLinkId, link.getId())
                            .orderByAsc(EcListingLinkSku::getSortOrder));
            for (EcListingLinkSku sku : skus) {
                try {
                    NormalizedSku normalized = normalizeExistingSku(sku, shop);
                    sku.setBaseCostAmount(normalized.baseCostAmount);
                    sku.setPlatformFeeAmount(normalized.platformFeeAmount);
                    sku.setCostPrice(normalized.costPrice);
                    sku.setMinSetAmount(normalized.minSetAmount);
                    sku.setProfit(normalized.profit);
                    ecListingLinkSkuMapper.updateById(sku);
                    updated++;
                } catch (Exception ex) {
                    log.warn("重算链接 SKU 定价失败 linkId={} skuId={}: {}",
                            link.getId(), sku.getId(), ex.getMessage());
                }
            }
        }
        log.info("上架链接定价批量重算完成，更新 {} 条 SKU", updated);
        return updated;
    }

    private NormalizedSku normalizeExistingSku(EcListingLinkSku item, EcShop shop) {
        String skuCodes = listingLinkSkuSupport.normalizeSkuCodes(item.getSkuCodes());
        BigDecimal discountPct = item.getDiscountPct() != null ? item.getDiscountPct() : new BigDecimal("100");
        BigDecimal couponAmount = item.getCouponAmount() != null ? item.getCouponAmount() : BigDecimal.ZERO;
        EcListingLinkCostBreakdown breakdown = listingLinkSkuSupport.calculateCostBreakdown(skuCodes, shop);
        BigDecimal minSetAmount = listingLinkSkuSupport.calculateMinSetAmount(
                breakdown.getCostPrice(), discountPct, couponAmount);
        BigDecimal profit = listingLinkSkuSupport.calculateProfit(
                item.getActualSetAmount(), breakdown.getBaseCostAmount(), shop, discountPct, couponAmount);
        NormalizedSku sku = new NormalizedSku();
        sku.baseCostAmount = breakdown.getBaseCostAmount();
        sku.platformFeeAmount = breakdown.getPlatformFeeAmount();
        sku.costPrice = breakdown.getCostPrice();
        sku.minSetAmount = minSetAmount;
        sku.profit = profit;
        return sku;
    }

    private Set<Long> resolveLinkIdsByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String kw = keyword.trim();
        Set<Long> ids = new HashSet<>();

        list(new LambdaQueryWrapper<EcListingLink>().and(w -> w
                .like(EcListingLink::getName, kw)
                .or().like(EcListingLink::getRemark, kw)))
                .forEach(link -> ids.add(link.getId()));

        ecListingLinkSkuMapper.selectList(new LambdaQueryWrapper<EcListingLinkSku>().and(w -> w
                .like(EcListingLinkSku::getSkuName, kw)
                .or().like(EcListingLinkSku::getSkuCodes, kw)))
                .forEach(sku -> ids.add(sku.getLinkId()));

        List<EcSku> matchedSkus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>().and(w -> w
                .like(EcSku::getSkuCode, kw)
                .or().like(EcSku::getSpecName, kw)));
        appendLinkIdsBySkuCodes(ids, matchedSkus.stream().map(EcSku::getSkuCode).toList());

        List<EcProduct> matchedProducts = ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                .like(EcProduct::getName, kw));
        if (!matchedProducts.isEmpty()) {
            Set<Long> productIds = matchedProducts.stream().map(EcProduct::getId).collect(Collectors.toSet());
            ecListingLinkProductMapper.selectList(new LambdaQueryWrapper<EcListingLinkProduct>()
                            .in(EcListingLinkProduct::getProductId, productIds))
                    .forEach(rel -> ids.add(rel.getLinkId()));
            List<EcSku> productSkus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                    .in(EcSku::getProductId, productIds));
            appendLinkIdsBySkuCodes(ids, productSkus.stream().map(EcSku::getSkuCode).toList());
        }

        List<EcFactory> matchedFactories = ecFactoryMapper.selectList(new LambdaQueryWrapper<EcFactory>()
                .like(EcFactory::getName, kw));
        if (!matchedFactories.isEmpty()) {
            Set<Long> factoryIds = matchedFactories.stream().map(EcFactory::getId).collect(Collectors.toSet());
            List<EcProduct> factoryProducts = ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                    .in(EcProduct::getFactoryId, factoryIds));
            if (!factoryProducts.isEmpty()) {
                Set<Long> productIds = factoryProducts.stream().map(EcProduct::getId).collect(Collectors.toSet());
                ecListingLinkProductMapper.selectList(new LambdaQueryWrapper<EcListingLinkProduct>()
                                .in(EcListingLinkProduct::getProductId, productIds))
                        .forEach(rel -> ids.add(rel.getLinkId()));
                List<EcSku> factorySkus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                        .in(EcSku::getProductId, productIds));
                appendLinkIdsBySkuCodes(ids, factorySkus.stream().map(EcSku::getSkuCode).toList());
            }
        }
        return ids;
    }

    private void appendLinkIdsBySkuCodes(Set<Long> ids, List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return;
        }
        List<EcListingLinkSku> linkSkus = ecListingLinkSkuMapper.selectList(new LambdaQueryWrapper<>());
        for (EcListingLinkSku linkSku : linkSkus) {
            if (!StringUtils.hasText(linkSku.getSkuCodes())) {
                continue;
            }
            for (String code : skuCodes) {
                if (linkSku.getSkuCodes().contains(code)) {
                    ids.add(linkSku.getLinkId());
                    break;
                }
            }
        }
    }

    private void applyLinkFields(EcListingLink link, EcListingLinkSaveRequest request, EcShop shop) {
        link.setName(request.getName().trim());
        link.setShopId(shop.getId());
        link.setPlatformUrl(trimToNull(request.getPlatformUrl()));
        link.setListingTime(request.getListingTime() != null ? request.getListingTime() : LocalDateTime.now());
        link.setRemark(trimToNull(request.getRemark()));
        link.setStatus(StringUtils.hasText(request.getStatus())
                ? request.getStatus().trim().toUpperCase() : "ENABLED");
    }

    private void validateProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Long> distinct = productIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return;
        }
        Map<Long, EcProduct> productMap = loadProductMap(distinct);
        for (Long productId : distinct) {
            if (!productMap.containsKey(productId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "关联商品不存在");
            }
        }
    }

    private void saveLinkProducts(Long linkId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Long> distinct = productIds.stream().filter(Objects::nonNull).distinct().toList();
        int sort = 0;
        for (Long productId : distinct) {
            EcListingLinkProduct rel = new EcListingLinkProduct();
            rel.setLinkId(linkId);
            rel.setProductId(productId);
            rel.setSortOrder(sort++);
            ecListingLinkProductMapper.insert(rel);
        }
    }

    private List<EcListingLinkProductVO> loadLinkProducts(Long linkId) {
        return loadLinkProductsMap(List.of(linkId)).getOrDefault(linkId, List.of());
    }

    private Map<Long, List<EcListingLinkProductVO>> loadLinkProductsMap(List<Long> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Map.of();
        }
        List<EcListingLinkProduct> relations = ecListingLinkProductMapper.selectList(
                new LambdaQueryWrapper<EcListingLinkProduct>()
                        .in(EcListingLinkProduct::getLinkId, linkIds)
                        .orderByAsc(EcListingLinkProduct::getSortOrder)
                        .orderByAsc(EcListingLinkProduct::getId));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, EcProduct> productMap = loadProductMap(relations.stream()
                .map(EcListingLinkProduct::getProductId).distinct().toList());
        Map<Long, List<EcListingLinkProductVO>> result = new java.util.HashMap<>();
        for (EcListingLinkProduct rel : relations) {
            EcListingLinkProductVO vo = new EcListingLinkProductVO();
            vo.setProductId(rel.getProductId());
            vo.setSortOrder(rel.getSortOrder());
            EcProduct product = productMap.get(rel.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
            }
            result.computeIfAbsent(rel.getLinkId(), k -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    private Set<Long> resolveShopIds(Long shopId, Long platformId) {
        if (shopId != null) {
            return Set.of(shopId);
        }
        if (platformId == null) {
            return null;
        }
        List<EcShop> shops = ecShopMapper.selectList(new LambdaQueryWrapper<EcShop>()
                .eq(EcShop::getPlatformId, platformId));
        return shops.stream().map(EcShop::getId).collect(Collectors.toSet());
    }

    private EcShop requireShop(Long shopId) {
        if (shopId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择店铺");
        }
        EcShop shop = ecShopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "店铺不存在");
        }
        return shop;
    }

    private List<NormalizedSku> validateAndNormalizeSkus(EcListingLinkSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "链接名称不能为空");
        }
        if (request.getShopId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择店铺");
        }
        if (request.getSkus() == null || request.getSkus().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请至少添加一条链接 SKU");
        }
        EcShop shop = requireShop(request.getShopId());
        List<NormalizedSku> normalized = new ArrayList<>();
        int sort = 0;
        for (EcListingLinkSkuItem item : request.getSkus()) {
            if (item == null || !StringUtils.hasText(item.getSkuName())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "链接 SKU 名称不能为空");
            }
            if (item.getActualSetAmount() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "真实设置金额不能为空");
            }
            String skuCodes = listingLinkSkuSupport.normalizeSkuCodes(item.getSkuCodes());
            BigDecimal discountPct = item.getDiscountPct() != null ? item.getDiscountPct() : new BigDecimal("100");
            BigDecimal couponAmount = item.getCouponAmount() != null ? item.getCouponAmount() : BigDecimal.ZERO;
            EcListingLinkCostBreakdown breakdown = listingLinkSkuSupport.calculateCostBreakdown(skuCodes, shop);
            BigDecimal minSetAmount = listingLinkSkuSupport.calculateMinSetAmount(
                    breakdown.getCostPrice(), discountPct, couponAmount);
            BigDecimal profit = listingLinkSkuSupport.calculateProfit(
                    item.getActualSetAmount(), breakdown.getBaseCostAmount(), shop, discountPct, couponAmount);

            NormalizedSku sku = new NormalizedSku();
            sku.skuName = item.getSkuName().trim();
            sku.skuCodes = skuCodes;
            sku.discountPct = discountPct;
            sku.couponAmount = couponAmount;
            sku.minSetAmount = minSetAmount;
            sku.baseCostAmount = breakdown.getBaseCostAmount();
            sku.platformFeeAmount = breakdown.getPlatformFeeAmount();
            sku.costPrice = breakdown.getCostPrice();
            sku.actualSetAmount = item.getActualSetAmount();
            sku.profit = profit;
            sku.sortOrder = item.getSortOrder() != null ? item.getSortOrder() : sort++;
            normalized.add(sku);
        }
        return normalized;
    }

    private void saveSkus(Long linkId, List<NormalizedSku> skus) {
        for (NormalizedSku item : skus) {
            EcListingLinkSku entity = new EcListingLinkSku();
            entity.setLinkId(linkId);
            entity.setSkuName(item.skuName);
            entity.setSkuCodes(item.skuCodes);
            entity.setDiscountPct(item.discountPct);
            entity.setCouponAmount(item.couponAmount);
            entity.setMinSetAmount(item.minSetAmount);
            entity.setBaseCostAmount(item.baseCostAmount);
            entity.setPlatformFeeAmount(item.platformFeeAmount);
            entity.setCostPrice(item.costPrice);
            entity.setActualSetAmount(item.actualSetAmount);
            entity.setProfit(item.profit);
            entity.setSortOrder(item.sortOrder);
            ecListingLinkSkuMapper.insert(entity);
        }
    }

    private List<EcListingLinkSkuVO> loadSkus(Long linkId, EcShop shop) {
        List<EcListingLinkSku> skus = ecListingLinkSkuMapper.selectList(
                new LambdaQueryWrapper<EcListingLinkSku>()
                        .eq(EcListingLinkSku::getLinkId, linkId)
                        .orderByAsc(EcListingLinkSku::getSortOrder)
                        .orderByAsc(EcListingLinkSku::getId));
        List<EcListingLinkSkuVO> result = new ArrayList<>();
        for (EcListingLinkSku sku : skus) {
            EcListingLinkSkuVO vo = new EcListingLinkSkuVO();
            vo.setId(sku.getId());
            vo.setSkuName(sku.getSkuName());
            vo.setSkuCodes(sku.getSkuCodes());
            vo.setDiscountPct(sku.getDiscountPct());
            vo.setCouponAmount(sku.getCouponAmount());
            vo.setMinSetAmount(sku.getMinSetAmount());
            vo.setBaseCostAmount(sku.getBaseCostAmount());
            vo.setPlatformFeeAmount(sku.getPlatformFeeAmount());
            vo.setCostPrice(sku.getCostPrice());
            vo.setActualSetAmount(sku.getActualSetAmount());
            vo.setProfit(sku.getProfit());
            vo.setSortOrder(sku.getSortOrder());
            vo.setInventories(listingLinkInventorySupport.loadInventories(sku.getSkuCodes()));
            try {
                EcListingLinkCostBreakdown breakdown = listingLinkSkuSupport.calculateCostBreakdown(
                        sku.getSkuCodes(), shop);
                vo.setSkuAmount(breakdown.getSkuAmount());
                vo.setCartonAmount(breakdown.getCartonAmount());
                vo.setExpressAmount(breakdown.getExpressAmount());
                vo.setPlatformFeePct(breakdown.getPlatformFeePct());
                vo.setProvinceName(breakdown.getProvinceName());
            } catch (BusinessException ignored) {
                vo.setSkuAmount(null);
                vo.setCartonAmount(null);
                vo.setExpressAmount(null);
            }
            vo.setPricingRisk(listingLinkSkuSupport.resolvePricingRisk(
                    sku.getActualSetAmount(), sku.getMinSetAmount(), sku.getProfit()));
            result.add(vo);
        }
        return result;
    }

    private Map<Long, Long> loadSkuCountMap(List<Long> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Map.of();
        }
        List<EcListingLinkSku> skus = ecListingLinkSkuMapper.selectList(
                new LambdaQueryWrapper<EcListingLinkSku>().in(EcListingLinkSku::getLinkId, linkIds));
        Map<Long, Long> result = new java.util.HashMap<>();
        for (EcListingLinkSku sku : skus) {
            result.merge(sku.getLinkId(), 1L, Long::sum);
        }
        return result;
    }

    private Map<Long, EcShop> loadShopMap(List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Map.of();
        }
        return ecShopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(EcShop::getId, s -> s, (a, b) -> a));
    }

    private Map<Long, EcPlatform> loadPlatformMap(List<Long> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return Map.of();
        }
        return ecPlatformMapper.selectBatchIds(platformIds).stream()
                .collect(Collectors.toMap(EcPlatform::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, EcProduct> loadProductMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return ecProductMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(EcProduct::getId, p -> p, (a, b) -> a));
    }

    private EcListingLinkDetailVO toDetailVO(EcListingLink link, EcShop shop, EcPlatform platform,
                                             List<EcListingLinkProductVO> products,
                                             List<EcListingLinkSkuVO> skus) {
        EcListingLinkDetailVO vo = new EcListingLinkDetailVO();
        vo.setId(link.getId());
        vo.setName(link.getName());
        vo.setShopId(link.getShopId());
        vo.setPlatformUrl(link.getPlatformUrl());
        vo.setProducts(products != null ? products : List.of());
        if (products != null && !products.isEmpty()) {
            vo.setProductNames(products.stream()
                    .map(EcListingLinkProductVO::getProductName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("、")));
        }
        if (shop != null) {
            vo.setShopName(shop.getName());
            vo.setPlatformId(shop.getPlatformId());
        }
        if (platform != null) {
            vo.setPlatformName(platform.getName());
        }
        vo.setListingTime(link.getListingTime());
        vo.setRemark(link.getRemark());
        vo.setStatus(link.getStatus());
        vo.setCreateTime(link.getCreateTime());
        vo.setUpdateTime(link.getUpdateTime());
        vo.setSkus(skus);
        if (skus != null) {
            vo.setSkuCount(skus.size());
        }
        return vo;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static final class NormalizedSku {
        private String skuName;
        private String skuCodes;
        private BigDecimal discountPct;
        private BigDecimal couponAmount;
        private BigDecimal minSetAmount;
        private BigDecimal baseCostAmount;
        private BigDecimal platformFeeAmount;
        private BigDecimal costPrice;
        private BigDecimal actualSetAmount;
        private BigDecimal profit;
        private Integer sortOrder;
    }
}
