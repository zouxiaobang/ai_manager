package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcProductSaveRequest;
import com.ai.manager.system.domain.dto.EcSkuSaveItem;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.EcProductDetailVO;
import com.ai.manager.system.domain.vo.EcProductListItemVO;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.service.EcProductService;
import com.ai.manager.system.service.EcEcommerceImageRenameService;
import com.ai.manager.system.service.EcSystemSettingsService;
import com.ai.manager.system.service.support.EcCartonMatcher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EcProductServiceImpl extends ServiceImpl<EcProductMapper, EcProduct> implements EcProductService {

    private final EcSkuMapper ecSkuMapper;
    private final EcFactoryMapper ecFactoryMapper;
    private final EcCartonMapper ecCartonMapper;
    private final EcSystemSettingsService ecSystemSettingsService;
    private final EcEcommerceImageRenameService ecEcommerceImageRenameService;

    private static final String PRODUCT_LIST_ORDER_SQL =
            "ORDER BY CASE WHEN status = 'ENABLED' THEN 0 ELSE 1 END ASC, "
                    + "(SELECT GREATEST("
                    + "IFNULL(MAX(l.create_time), '1970-01-01 00:00:00'), "
                    + "IFNULL(MAX(i.update_time), '1970-01-01 00:00:00')) "
                    + "FROM ec_sku s "
                    + "INNER JOIN ec_inventory i ON i.sku_code = s.sku_code AND i.deleted = 0 "
                    + "LEFT JOIN ec_inventory_log l ON l.inventory_id = i.id AND l.deleted = 0 "
                    + "WHERE s.product_id = ec_product.id AND s.deleted = 0) DESC, "
                    + "(SELECT COALESCE(SUM(i.quantity), 0) FROM ec_sku s "
                    + "INNER JOIN ec_inventory i ON i.sku_code = s.sku_code AND i.deleted = 0 "
                    + "WHERE s.product_id = ec_product.id AND s.deleted = 0) DESC, "
                    + "create_time ASC, id ASC";

    @Override
    public PageResult<EcProductListItemVO> pageProducts(String keyword, Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            Set<Long> matchedIds = collectProductIdsByKeyword(kw);
            if (matchedIds.isEmpty()) {
                return PageResult.empty(p, ps);
            }
            wrapper.in(EcProduct::getId, matchedIds);
        }
        wrapper.last(PRODUCT_LIST_ORDER_SQL);
        Page<EcProduct> entityPage = page(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            return PageUtils.of(List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        }
        List<EcProductListItemVO> records = mapProductsToListItems(entityPage.getRecords());
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    private List<EcProductListItemVO> mapProductsToListItems(List<EcProduct> products) {
        List<Long> productIds = products.stream().map(EcProduct::getId).toList();
        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .in(EcSku::getProductId, productIds));
        Map<Long, Long> skuCountMap = skus.stream()
                .collect(Collectors.groupingBy(EcSku::getProductId, Collectors.counting()));

        Map<Long, String> factoryNameMap = loadFactoryNameMap(products.stream()
                .map(EcProduct::getFactoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        List<EcProductListItemVO> result = new ArrayList<>();
        for (EcProduct product : products) {
            EcProductListItemVO item = new EcProductListItemVO();
            item.setId(product.getId());
            item.setName(product.getName());
            item.setImageName(product.getImageName());
            item.setFactoryId(product.getFactoryId());
            if (product.getFactoryId() != null) {
                item.setFactoryName(factoryNameMap.get(product.getFactoryId()));
            }
            item.setRebatePct(product.getRebatePct());
            item.setStatus(product.getStatus());
            item.setUpdateTime(product.getUpdateTime());
            item.setSkuCount(skuCountMap.getOrDefault(product.getId(), 0L).intValue());
            result.add(item);
        }
        return result;
    }

    @Override
    public EcProductDetailVO getProductDetail(Long id) {
        EcProduct product = getById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toDetailVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcProductDetailVO createProduct(EcProductSaveRequest request) {
        validateProductRequest(request, true);
        EcProduct product = new EcProduct();
        applyProductFields(request, product);
        save(product);
        syncSkus(product.getId(), request.getSkus(), true, product);
        return getProductDetail(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcProductDetailVO updateProduct(Long id, EcProductSaveRequest request) {
        EcProduct existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateProductRequest(request, false);
        applyProductFields(request, existing);
        updateById(existing);
        syncSkus(id, request.getSkus(), false, existing);
        return getProductDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        EcProduct existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        removeById(id);
        ecSkuMapper.delete(new LambdaQueryWrapper<EcSku>().eq(EcSku::getProductId, id));
    }

    private Set<Long> collectProductIdsByKeyword(String keyword) {
        Set<Long> matchedIds = new HashSet<>();

        list(new LambdaQueryWrapper<EcProduct>().like(EcProduct::getName, keyword))
                .forEach(product -> matchedIds.add(product.getId()));

        ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>().like(EcSku::getSpecName, keyword))
                .forEach(sku -> matchedIds.add(sku.getProductId()));

        List<EcFactory> factories = ecFactoryMapper.selectList(
                new LambdaQueryWrapper<EcFactory>().like(EcFactory::getName, keyword));
        if (!factories.isEmpty()) {
            List<Long> factoryIds = factories.stream().map(EcFactory::getId).toList();
            list(new LambdaQueryWrapper<EcProduct>().in(EcProduct::getFactoryId, factoryIds))
                    .forEach(product -> matchedIds.add(product.getId()));
        }

        return matchedIds;
    }

    private EcProductDetailVO toDetailVO(EcProduct product) {
        EcProductDetailVO vo = new EcProductDetailVO();
        BeanUtils.copyProperties(product, vo);
        if (product.getFactoryId() != null) {
            EcFactory factory = ecFactoryMapper.selectById(product.getFactoryId());
            if (factory != null) {
                vo.setFactoryName(factory.getName());
            }
        }
        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .eq(EcSku::getProductId, product.getId())
                .orderByAsc(EcSku::getId));
        fillSkuCartonNames(skus);
        vo.setSkus(skus);
        return vo;
    }

    private void applyProductFields(EcProductSaveRequest request, EcProduct product) {
        product.setName(request.getName().trim());
        product.setFactoryId(resolveFactoryId(request.getFactoryId()));
        product.setDescription(trimToNull(request.getDescription()));
        product.setRebatePct(normalizeRebatePct(request.getRebatePct()));
        product.setImageName(ecEcommerceImageRenameService.normalizeSpuMainImage(
                request.getImageName(), request.getName().trim()));
        if (StringUtils.hasText(request.getStatus())) {
            product.setStatus(request.getStatus().trim().toUpperCase());
        } else if (product.getStatus() == null) {
            product.setStatus("ENABLED");
        }
    }

    private void syncSkus(Long productId, List<EcSkuSaveItem> items, boolean creating, EcProduct product) {
        if (items == null || items.isEmpty()) {
            if (creating) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "至少添加一个 SKU");
            }
            ecSkuMapper.delete(new LambdaQueryWrapper<EcSku>().eq(EcSku::getProductId, productId));
            return;
        }

        validateSkuItems(items, product.getRebatePct());

        Set<Long> keepIds = new HashSet<>();
        for (EcSkuSaveItem item : items) {
            EcSku existing = null;
            if (item.getId() != null) {
                existing = ecSkuMapper.selectById(item.getId());
                if (existing == null || !Objects.equals(existing.getProductId(), productId)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 不存在或不属于该商品");
                }
            }
            EcSku sku = toSkuEntity(item, productId, product, existing);
            if (item.getId() != null) {
                sku.setId(item.getId());
                ecSkuMapper.updateById(sku);
                keepIds.add(item.getId());
            } else {
                ecSkuMapper.insert(sku);
                keepIds.add(sku.getId());
            }
        }

        if (!creating) {
            List<EcSku> existingSkus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                    .eq(EcSku::getProductId, productId));
            for (EcSku existing : existingSkus) {
                if (!keepIds.contains(existing.getId())) {
                    ecSkuMapper.deleteById(existing.getId());
                }
            }
        }
    }

    private EcSku toSkuEntity(EcSkuSaveItem item, Long productId, EcProduct product, EcSku existing) {
        EcSku sku = new EcSku();
        sku.setProductId(productId);
        sku.setSkuCode(item.getSkuCode().trim());
        sku.setSpecName(trimToNull(item.getSpecName()));
        sku.setRebatePct(resolveSkuRebatePct(item.getRebatePct(), product.getRebatePct()));
        sku.setImageName(ecEcommerceImageRenameService.normalizeSkuImage(
                item.getImageName(),
                product.getName(),
                item.getSpecName(),
                item.getSkuCode(),
                item.getId()));
        sku.setSalePrice(item.getSalePrice());
        sku.setProductLengthCm(item.getProductLengthCm());
        sku.setProductWidthCm(item.getProductWidthCm());
        sku.setProductHeightCm(item.getProductHeightCm());
        sku.setCartonLengthCm(item.getCartonLengthCm());
        sku.setCartonWidthCm(item.getCartonWidthCm());
        sku.setCartonHeightCm(item.getCartonHeightCm());
        sku.setCartonGrossWeightKg(item.getCartonGrossWeightKg());
        sku.setCartonNetWeightKg(item.getCartonNetWeightKg());
        sku.setUnitsPerCarton(item.getUnitsPerCarton() == null || item.getUnitsPerCarton() < 1
                ? 1
                : item.getUnitsPerCarton());
        sku.setCartonId(resolveSkuCartonId(item, product, existing));
        if (StringUtils.hasText(item.getStatus())) {
            sku.setStatus(item.getStatus().trim().toUpperCase());
        } else {
            sku.setStatus("ON_SALE");
        }
        return sku;
    }

    private Long resolveSkuCartonId(EcSkuSaveItem item, EcProduct product, EcSku existing) {
        if (item.getId() != null) {
            if (item.getCartonId() != null) {
                return resolveCartonId(item.getCartonId());
            }
            return existing != null ? existing.getCartonId() : null;
        }
        if (item.getCartonId() != null) {
            return resolveCartonId(item.getCartonId());
        }
        return matchCartonForSku(item, product.getFactoryId());
    }

    private Long matchCartonForSku(EcSkuSaveItem item, Long factoryId) {
        List<EcCarton> all = ecCartonMapper.selectList(new LambdaQueryWrapper<EcCarton>()
                .orderByAsc(EcCarton::getId));
        List<EcCarton> preferred = EcCartonMatcher.preferFactoryCartons(all, factoryId);
        EcCarton matched = EcCartonMatcher.findBestFit(preferred,
                item.getProductLengthCm(), item.getProductWidthCm(), item.getProductHeightCm());
        if (matched == null && factoryId != null) {
            matched = EcCartonMatcher.findBestFit(all,
                    item.getProductLengthCm(), item.getProductWidthCm(), item.getProductHeightCm());
        }
        return matched != null ? matched.getId() : null;
    }

    private Long resolveCartonId(Long cartonId) {
        EcCarton carton = ecCartonMapper.selectById(cartonId);
        if (carton == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "纸箱不存在");
        }
        return cartonId;
    }

    private void fillSkuCartonNames(List<EcSku> skus) {
        if (skus == null || skus.isEmpty()) {
            return;
        }
        List<Long> cartonIds = skus.stream()
                .map(EcSku::getCartonId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (cartonIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = ecCartonMapper.selectBatchIds(cartonIds).stream()
                .collect(Collectors.toMap(EcCarton::getId, EcCarton::getName, (a, b) -> a));
        for (EcSku sku : skus) {
            if (sku.getCartonId() != null) {
                sku.setCartonName(nameMap.get(sku.getCartonId()));
            }
        }
    }

    private void validateProductRequest(EcProductSaveRequest request, boolean creating) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品名称不能为空");
        }
        BigDecimal rebate = normalizeRebatePct(request.getRebatePct());
        if (rebate.compareTo(BigDecimal.ZERO) < 0 || rebate.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "退点须在 0～100 之间");
        }
        if (creating && (request.getSkus() == null || request.getSkus().isEmpty())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "至少添加一个 SKU");
        }
    }

    private void validateSkuItems(List<EcSkuSaveItem> items, BigDecimal productRebatePct) {
        Set<String> codes = new HashSet<>();
        for (EcSkuSaveItem item : items) {
            if (item == null || !StringUtils.hasText(item.getSkuCode())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "货号不能为空");
            }
            BigDecimal rebate = resolveSkuRebatePct(item.getRebatePct(), productRebatePct);
            if (rebate.compareTo(BigDecimal.ZERO) < 0 || rebate.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 退点须在 0～100 之间");
            }
            String code = item.getSkuCode().trim();
            if (!codes.add(code)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "货号重复: " + code);
            }
            EcSku conflict = ecSkuMapper.selectOne(new LambdaQueryWrapper<EcSku>()
                    .eq(EcSku::getSkuCode, code)
                    .ne(item.getId() != null, EcSku::getId, item.getId())
                    .last("LIMIT 1"));
            if (conflict != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "货号已存在: " + code);
            }
            if (item.getUnitsPerCarton() != null && item.getUnitsPerCarton() < 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "箱装数量至少为 1");
            }
        }
    }

    private Long resolveFactoryId(Long factoryId) {
        if (factoryId == null) {
            return null;
        }
        EcFactory factory = ecFactoryMapper.selectById(factoryId);
        if (factory == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工厂不存在");
        }
        return factoryId;
    }

    private BigDecimal resolveSkuRebatePct(BigDecimal skuRebatePct, BigDecimal productRebatePct) {
        if (skuRebatePct != null) {
            return normalizeRebatePct(skuRebatePct);
        }
        return normalizeRebatePct(productRebatePct);
    }

    private Map<Long, String> loadFactoryNameMap(List<Long> factoryIds) {
        if (factoryIds == null || factoryIds.isEmpty()) {
            return Map.of();
        }
        return ecFactoryMapper.selectBatchIds(factoryIds).stream()
                .collect(Collectors.toMap(EcFactory::getId, EcFactory::getName, (a, b) -> a));
    }

    private BigDecimal normalizeRebatePct(BigDecimal rebatePct) {
        if (rebatePct == null) {
            return ecSystemSettingsService.resolveDefaultRebatePct();
        }
        return rebatePct;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
