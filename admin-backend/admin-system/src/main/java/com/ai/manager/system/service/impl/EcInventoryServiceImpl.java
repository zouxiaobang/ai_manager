package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcInventoryAdjustRequest;
import com.ai.manager.system.domain.dto.EcInventoryInboundRequest;
import com.ai.manager.system.domain.dto.EcInventoryOutboundRequest;
import com.ai.manager.system.domain.dto.EcInventorySaveRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcInventory;
import com.ai.manager.system.domain.entity.EcInventoryLog;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcInboundOrder;
import com.ai.manager.system.domain.entity.EcInboundOrderLine;
import com.ai.manager.system.domain.entity.EcOutboundOrder;
import com.ai.manager.system.domain.entity.EcOutboundOrderLine;
import com.ai.manager.system.domain.vo.EcInventoryDetailVO;
import com.ai.manager.system.domain.vo.EcInventoryFactorySummaryVO;
import com.ai.manager.system.domain.vo.EcInventoryGlobalLogVO;
import com.ai.manager.system.domain.vo.EcInventoryInboundBriefVO;
import com.ai.manager.system.domain.vo.EcInventoryInboundValueSummaryVO;
import com.ai.manager.system.domain.vo.EcInventoryOutboundBriefVO;
import com.ai.manager.system.domain.vo.EcInventoryListItemVO;
import com.ai.manager.system.domain.vo.EcInventoryLogVO;
import com.ai.manager.system.domain.vo.EcInventoryPackingEstimateVO;
import com.ai.manager.system.domain.vo.EcInventorySkuOptionVO;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcInboundOrderLineMapper;
import com.ai.manager.system.mapper.EcOutboundOrderLineMapper;
import com.ai.manager.system.mapper.EcOutboundOrderMapper;
import com.ai.manager.system.mapper.EcInboundOrderMapper;
import com.ai.manager.system.mapper.EcInventoryLogMapper;
import com.ai.manager.system.mapper.EcInventoryMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.service.EcInventoryService;
import com.ai.manager.system.service.EcSystemSettingsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EcInventoryServiceImpl extends ServiceImpl<EcInventoryMapper, EcInventory> implements EcInventoryService {

    private static final String CHANGE_DEDUCT = "DEDUCT";
    private static final String CHANGE_RECLAIM = "RECLAIM";
    private static final String CHANGE_INBOUND = "INBOUND";
    private static final String CHANGE_STOCKTAKE = "STOCKTAKE";
    private static final String REF_INBOUND_ORDER = "INBOUND_ORDER";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String SKU_ON_SALE = "ON_SALE";
    private static final String PRODUCT_ENABLED = "ENABLED";
    private static final int RECENT_LOG_LIMIT = 5;

    private final EcSystemSettingsService ecSystemSettingsService;
    private final EcInventoryLogMapper ecInventoryLogMapper;
    private final EcInboundOrderMapper ecInboundOrderMapper;
    private final EcInboundOrderLineMapper ecInboundOrderLineMapper;
    private final EcOutboundOrderMapper ecOutboundOrderMapper;
    private final EcOutboundOrderLineMapper ecOutboundOrderLineMapper;
    private final EcCartonMapper ecCartonMapper;
    private final EcSkuMapper ecSkuMapper;
    private final EcProductMapper ecProductMapper;
    private final EcFactoryMapper ecFactoryMapper;

    @Override
    public PageResult<EcInventoryListItemVO> pageInventories(String keyword, Boolean alertOnly, Boolean inStockOnly,
                                                               Long factoryId, Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcInventory> wrapper = buildInventoryQueryWrapper(keyword, factoryId, alertOnly, inStockOnly);

        Set<String> allowedSkuCodes = resolveAllowedSkuCodes(keyword, factoryId);
        if (allowedSkuCodes != null && allowedSkuCodes.isEmpty()) {
            return PageResult.empty(p, ps);
        }

        Map<String, Object> extra = buildInventorySummary(wrapper);

        Page<EcInventory> entityPage = page(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            PageResult<EcInventoryListItemVO> empty = PageUtils.of(
                    List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
            empty.setExtra(extra);
            return empty;
        }

        Map<String, SkuBrief> skuBriefMap = loadSkuBriefMap(
                entityPage.getRecords().stream().map(EcInventory::getSkuCode).toList());
        Map<String, Integer> inTransitMap = loadInTransitMap(
                entityPage.getRecords().stream().map(EcInventory::getSkuCode).toList());
        List<EcInventoryListItemVO> records = new ArrayList<>();
        for (EcInventory inventory : entityPage.getRecords()) {
            EcInventoryListItemVO item = toListItemVO(
                    inventory, skuBriefMap.get(inventory.getSkuCode()), List.of());
            item.setInTransitQty(inTransitMap.getOrDefault(inventory.getSkuCode(), 0));
            records.add(item);
        }
        PageResult<EcInventoryListItemVO> result = PageUtils.of(
                records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        result.setExtra(extra);
        return result;
    }

    private LambdaQueryWrapper<EcInventory> buildInventoryQueryWrapper(String keyword, Long factoryId,
                                                                       Boolean alertOnly, Boolean inStockOnly) {
        LambdaQueryWrapper<EcInventory> wrapper = new LambdaQueryWrapper<EcInventory>()
                .orderByDesc(EcInventory::getId);
        Set<String> allowedSkuCodes = resolveAllowedSkuCodes(keyword, factoryId);
        if (allowedSkuCodes != null) {
            wrapper.in(EcInventory::getSkuCode, allowedSkuCodes);
        }
        if (Boolean.TRUE.equals(inStockOnly)) {
            wrapper.gt(EcInventory::getQuantity, 0);
        }
        if (Boolean.TRUE.equals(alertOnly)) {
            wrapper.and(w -> w.isNull(EcInventory::getIgnoreAlert).or().eq(EcInventory::getIgnoreAlert, 0))
                    .apply("quantity <= IFNULL(alert_threshold, 0)");
        }
        return wrapper;
    }

    private Map<String, Object> buildInventorySummary(LambdaQueryWrapper<EcInventory> wrapper) {
        List<EcInventory> all = list(wrapper);
        if (all.isEmpty()) {
            Map<String, Object> extra = new HashMap<>();
            extra.put("totalQuantity", 0L);
            extra.put("totalStockValue", BigDecimal.ZERO);
            return extra;
        }
        Map<String, SkuBrief> skuBriefMap = loadSkuBriefMap(
                all.stream().map(EcInventory::getSkuCode).distinct().toList());
        long totalQuantity = 0;
        BigDecimal totalStockValue = BigDecimal.ZERO;
        for (EcInventory inventory : all) {
            int qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            totalQuantity += qty;
            SkuBrief brief = skuBriefMap.get(inventory.getSkuCode());
            if (brief != null && brief.salePrice != null) {
                totalStockValue = totalStockValue.add(
                        brief.salePrice.multiply(BigDecimal.valueOf(qty)));
            }
        }
        Map<String, Object> extra = new HashMap<>();
        extra.put("totalQuantity", totalQuantity);
        extra.put("totalStockValue", totalStockValue);
        return extra;
    }

    @Override
    public EcInventoryDetailVO getInventoryDetail(Long id) {
        EcInventory inventory = getById(id);
        if (inventory == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        SkuContext skuCtx = loadSkuContext(inventory.getSkuCode());
        List<EcInventoryLogVO> recentLogs = listLogs(id);
        if (recentLogs.size() > RECENT_LOG_LIMIT) {
            recentLogs = recentLogs.subList(0, RECENT_LOG_LIMIT);
        }
        return toDetailVO(inventory, skuCtx, recentLogs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInventoryListItemVO createInventory(EcInventorySaveRequest request) {
        validateSaveRequest(request, true);
        String skuCode = request.getSkuCode().trim();
        requireSkuExists(skuCode);
        if (count(new LambdaQueryWrapper<EcInventory>().eq(EcInventory::getSkuCode, skuCode)) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该货号已存在库存记录");
        }
        EcInventory inventory = applySaveFields(request, new EcInventory());
        inventory.setSkuCode(skuCode);
        save(inventory);
        return getInventoryDetail(inventory.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInventoryListItemVO updateInventory(Long id, EcInventorySaveRequest request) {
        EcInventory existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateSaveRequest(request, false);
        if (StringUtils.hasText(request.getSkuCode())) {
            String skuCode = request.getSkuCode().trim();
            requireSkuExists(skuCode);
            if (!skuCode.equals(existing.getSkuCode())
                    && count(new LambdaQueryWrapper<EcInventory>().eq(EcInventory::getSkuCode, skuCode)) > 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该货号已存在库存记录");
            }
            existing.setSkuCode(skuCode);
        }
        applySaveFields(request, existing);
        updateById(existing);
        return toListItemVO(existing, loadSkuBriefMap(List.of(existing.getSkuCode())).get(existing.getSkuCode()), List.of());
    }

    private EcInventoryListItemVO getInventoryListItem(Long id) {
        EcInventory inventory = getById(id);
        if (inventory == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, SkuBrief> skuBriefMap = loadSkuBriefMap(List.of(inventory.getSkuCode()));
        return toListItemVO(inventory, skuBriefMap.get(inventory.getSkuCode()), List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInventoryListItemVO adjustInventory(Long id, EcInventoryAdjustRequest request) {
        EcInventory existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String changeType = normalizeChangeType(request != null ? request.getChangeType() : null);
        int changeQty = requirePositiveQty(request != null ? request.getChangeQty() : null, "改动数量");

        int newQty;
        if (CHANGE_DEDUCT.equals(changeType)) {
            newQty = existing.getQuantity() - changeQty;
            if (newQty < 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "库存不足，无法扣除");
            }
        } else {
            newQty = existing.getQuantity() + changeQty;
        }

        existing.setQuantity(newQty);
        updateById(existing);
        insertInventoryLog(id, changeType, changeQty, null, null, null);
        return getInventoryListItem(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInventoryDetailVO quickInbound(EcInventoryInboundRequest request) {
        return inbound(request, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInventoryDetailVO inbound(EcInventoryInboundRequest request, String refType, Long refId) {
        validateInboundRequest(request);
        String skuCode = request.getSkuCode().trim();
        requireSkuAvailableForInbound(skuCode);
        int changeQty = requirePositiveQty(request.getQuantity(), "进货数量");

        EcInventory inventory = getOrCreateInventory(
                skuCode, request.getAlertThreshold(), request.getIgnoreAlert());
        inventory.setQuantity(inventory.getQuantity() + changeQty);
        updateById(inventory);

        insertInventoryLog(inventory.getId(), CHANGE_INBOUND, changeQty, refType, refId, request.getRemark());
        return getInventoryDetail(inventory.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInventoryDetailVO outbound(EcInventoryOutboundRequest request, String refType, Long refId) {
        if (request == null || !StringUtils.hasText(request.getSkuCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "货号不能为空");
        }
        String skuCode = request.getSkuCode().trim();
        requireSkuExists(skuCode);
        int changeQty = requirePositiveQty(request.getQuantity(), "出货数量");

        EcInventory inventory = getOne(new LambdaQueryWrapper<EcInventory>().eq(EcInventory::getSkuCode, skuCode));
        if (inventory == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该货号尚无库存记录");
        }
        if (inventory.getQuantity() < changeQty) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "库存不足，无法出货");
        }
        inventory.setQuantity(inventory.getQuantity() - changeQty);
        updateById(inventory);
        insertInventoryLog(inventory.getId(), CHANGE_DEDUCT, changeQty, refType, refId, request.getRemark());
        return getInventoryDetail(inventory.getId());
    }

    @Override
    public List<EcInventorySkuOptionVO> listSkuOptions(Long factoryId, Long productId, List<Long> productIds,
                                                       String keyword) {
        LambdaQueryWrapper<EcSku> skuWrapper = new LambdaQueryWrapper<EcSku>().orderByAsc(EcSku::getSkuCode);
        List<Long> filterProductIds = productIds != null && !productIds.isEmpty()
                ? productIds.stream().filter(Objects::nonNull).distinct().toList()
                : List.of();
        if (!filterProductIds.isEmpty()) {
            skuWrapper.in(EcSku::getProductId, filterProductIds);
        } else if (productId != null) {
            skuWrapper.eq(EcSku::getProductId, productId);
        } else if (factoryId != null) {
            List<EcProduct> products = ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                    .eq(EcProduct::getFactoryId, factoryId));
            if (products.isEmpty()) {
                return List.of();
            }
            Set<Long> factoryProductIds = products.stream().map(EcProduct::getId).collect(Collectors.toSet());
            skuWrapper.in(EcSku::getProductId, factoryProductIds);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            skuWrapper.and(w -> w.like(EcSku::getSkuCode, kw).or().like(EcSku::getSpecName, kw));
        }
        List<EcSku> skus = ecSkuMapper.selectList(skuWrapper);
        if (skus.isEmpty()) {
            return List.of();
        }

        Map<String, EcInventory> inventoryMap = list(new LambdaQueryWrapper<EcInventory>()
                .in(EcInventory::getSkuCode, skus.stream().map(EcSku::getSkuCode).toList()))
                .stream()
                .collect(Collectors.toMap(EcInventory::getSkuCode, inv -> inv, (a, b) -> a));

        Set<Long> skuProductIds = skus.stream().map(EcSku::getProductId).collect(Collectors.toSet());
        Map<Long, EcProduct> productMap = ecProductMapper.selectBatchIds(skuProductIds).stream()
                .collect(Collectors.toMap(EcProduct::getId, p -> p, (a, b) -> a));
        Set<Long> factoryIds = productMap.values().stream()
                .map(EcProduct::getFactoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> factoryNameMap = factoryIds.isEmpty() ? Map.of()
                : ecFactoryMapper.selectBatchIds(factoryIds).stream()
                .collect(Collectors.toMap(EcFactory::getId, EcFactory::getName, (a, b) -> a));

        List<EcInventorySkuOptionVO> result = new ArrayList<>();
        for (EcSku sku : skus) {
            EcProduct product = productMap.get(sku.getProductId());
            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim();
                boolean skuMatched = sku.getSkuCode().contains(kw)
                        || (sku.getSpecName() != null && sku.getSpecName().contains(kw));
                boolean productMatched = product != null
                        && product.getName() != null
                        && product.getName().contains(kw);
                if (!skuMatched && !productMatched) {
                    continue;
                }
            }

            EcInventorySkuOptionVO vo = new EcInventorySkuOptionVO();
            vo.setSkuCode(sku.getSkuCode());
            vo.setSpecName(sku.getSpecName());
            vo.setSkuStatus(sku.getStatus());
            vo.setInboundAllowed(isSkuAvailableForInbound(sku, product));
            if (StringUtils.hasText(sku.getImageName())) {
                vo.setImageName(sku.getImageName().trim());
            } else if (product != null && StringUtils.hasText(product.getImageName())) {
                vo.setImageName(product.getImageName().trim());
            }
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductId(product.getId());
                vo.setFactoryId(product.getFactoryId());
                if (product.getFactoryId() != null) {
                    vo.setFactoryName(factoryNameMap.get(product.getFactoryId()));
                }
            }
            EcInventory inventory = inventoryMap.get(sku.getSkuCode());
            if (inventory != null) {
                vo.setHasInventory(true);
                vo.setQuantity(inventory.getQuantity());
                vo.setAlertThreshold(inventory.getAlertThreshold());
                vo.setIgnoreAlert(inventory.getIgnoreAlert() != null && inventory.getIgnoreAlert() == 1);
            } else {
                vo.setHasInventory(false);
                vo.setQuantity(0);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInventory(Long id) {
        EcInventory existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ecInventoryLogMapper.delete(new LambdaQueryWrapper<EcInventoryLog>()
                .eq(EcInventoryLog::getInventoryId, id));
        removeById(id);
    }

    @Override
    public List<EcInventoryLogVO> listLogs(Long inventoryId) {
        requireInventoryExists(inventoryId);
        return ecInventoryLogMapper.selectList(new LambdaQueryWrapper<EcInventoryLog>()
                        .eq(EcInventoryLog::getInventoryId, inventoryId)
                        .orderByDesc(EcInventoryLog::getId))
                .stream()
                .map(this::toLogVO)
                .toList();
    }

    @Override
    public PageResult<EcInventoryLogVO> pageLogs(Long inventoryId, Long page, Long pageSize) {
        requireInventoryExists(inventoryId);
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        Page<EcInventoryLog> entityPage = ecInventoryLogMapper.selectPage(
                new Page<>(p, ps),
                new LambdaQueryWrapper<EcInventoryLog>()
                        .eq(EcInventoryLog::getInventoryId, inventoryId)
                        .orderByDesc(EcInventoryLog::getId));
        List<EcInventoryLogVO> records = entityPage.getRecords().stream()
                .map(this::toLogVO)
                .toList();
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public PageResult<EcInventoryGlobalLogVO> pageGlobalLogs(String keyword, String skuCode, Long factoryId,
                                                             String changeType, String refType,
                                                             LocalDateTime startTime, LocalDateTime endTime,
                                                             Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        Set<Long> inventoryIds = resolveGlobalLogInventoryIds(keyword, skuCode, factoryId);
        if (inventoryIds != null && inventoryIds.isEmpty()) {
            return PageResult.empty(p, ps);
        }

        LambdaQueryWrapper<EcInventoryLog> wrapper = new LambdaQueryWrapper<EcInventoryLog>()
                .orderByDesc(EcInventoryLog::getId);
        if (inventoryIds != null) {
            wrapper.in(EcInventoryLog::getInventoryId, inventoryIds);
        }
        if (StringUtils.hasText(changeType)) {
            wrapper.eq(EcInventoryLog::getChangeType, changeType.trim().toUpperCase());
        }
        if (StringUtils.hasText(refType)) {
            wrapper.eq(EcInventoryLog::getRefType, refType.trim().toUpperCase());
        }
        if (startTime != null) {
            wrapper.ge(EcInventoryLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(EcInventoryLog::getCreateTime, endTime);
        }

        Page<EcInventoryLog> entityPage = ecInventoryLogMapper.selectPage(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            return PageUtils.of(List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        }

        Map<Long, EcInventory> inventoryMap = listByIds(entityPage.getRecords().stream()
                        .map(EcInventoryLog::getInventoryId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(EcInventory::getId, inv -> inv, (a, b) -> a));
        List<String> skuCodes = inventoryMap.values().stream()
                .map(EcInventory::getSkuCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, SkuContext> skuContextMap = loadSkuContextMap(skuCodes);

        List<EcInventoryGlobalLogVO> records = new ArrayList<>();
        for (EcInventoryLog log : entityPage.getRecords()) {
            EcInventory inventory = inventoryMap.get(log.getInventoryId());
            SkuContext ctx = inventory != null ? skuContextMap.get(inventory.getSkuCode()) : null;
            records.add(toGlobalLogVO(log, inventory, ctx));
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public List<EcInventoryFactorySummaryVO> listFactorySummary(Long factoryId) {
        List<EcFactory> factories;
        if (factoryId != null) {
            EcFactory factory = ecFactoryMapper.selectById(factoryId);
            factories = factory == null ? List.of() : List.of(factory);
        } else {
            factories = ecFactoryMapper.selectList(new LambdaQueryWrapper<EcFactory>()
                    .orderByAsc(EcFactory::getId));
        }
        if (factories.isEmpty()) {
            return List.of();
        }

        List<EcProduct> allProducts = ecProductMapper.selectList(new LambdaQueryWrapper<>());

        Map<String, EcInventory> inventoryBySku = list().stream()
                .collect(Collectors.toMap(EcInventory::getSkuCode, inv -> inv, (a, b) -> a));

        List<EcInventoryFactorySummaryVO> result = new ArrayList<>();
        for (EcFactory factory : factories) {
            long skuCount = 0;
            long totalQuantity = 0;
            BigDecimal totalStockValue = BigDecimal.ZERO;
            long alertSkuCount = 0;

            for (EcProduct product : allProducts) {
                if (!factory.getId().equals(product.getFactoryId())) {
                    continue;
                }
                List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                        .eq(EcSku::getProductId, product.getId()));
                for (EcSku sku : skus) {
                    EcInventory inventory = inventoryBySku.get(sku.getSkuCode());
                    if (inventory == null) {
                        continue;
                    }
                    skuCount++;
                    int qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
                    totalQuantity += qty;
                    if (sku.getSalePrice() != null) {
                        totalStockValue = totalStockValue.add(
                                sku.getSalePrice().multiply(BigDecimal.valueOf(qty)));
                    }
                    if (isAlertActive(inventory)) {
                        alertSkuCount++;
                    }
                }
            }

            EcInventoryFactorySummaryVO vo = new EcInventoryFactorySummaryVO();
            vo.setFactoryId(factory.getId());
            vo.setFactoryName(factory.getName());
            vo.setSkuCount(skuCount);
            vo.setTotalQuantity(totalQuantity);
            vo.setTotalStockValue(totalStockValue);
            vo.setAlertSkuCount(alertSkuCount);
            result.add(vo);
        }
        return result;
    }

    @Override
    public EcInventoryInboundValueSummaryVO summarizeHistoricalInboundValue(Long factoryId) {
        BigDecimal total = sumInboundValueFromConfirmedOrders(factoryId)
                .add(sumInboundValueFromQuickInboundLogs(factoryId));
        EcInventoryInboundValueSummaryVO vo = new EcInventoryInboundValueSummaryVO();
        vo.setTotalInboundValue(total.setScale(2, RoundingMode.HALF_UP));
        return vo;
    }

    private BigDecimal sumInboundValueFromConfirmedOrders(Long factoryId) {
        LambdaQueryWrapper<EcInboundOrder> orderWrapper = new LambdaQueryWrapper<EcInboundOrder>()
                .eq(EcInboundOrder::getStatus, "CONFIRMED");
        if (factoryId != null) {
            orderWrapper.eq(EcInboundOrder::getFactoryId, factoryId);
        }
        List<EcInboundOrder> orders = ecInboundOrderMapper.selectList(orderWrapper);
        if (orders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<Long> orderIds = orders.stream().map(EcInboundOrder::getId).collect(Collectors.toSet());
        List<EcInboundOrderLine> lines = ecInboundOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcInboundOrderLine>()
                        .in(EcInboundOrderLine::getOrderId, orderIds));
        if (lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<String> skuCodes = lines.stream()
                .map(EcInboundOrderLine::getSkuCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
        Map<String, SkuBrief> skuBriefMap = loadSkuBriefMap(new ArrayList<>(skuCodes));

        BigDecimal total = BigDecimal.ZERO;
        for (EcInboundOrderLine line : lines) {
            int qty = line.getReceivedQuantity() != null ? line.getReceivedQuantity() : 0;
            if (qty <= 0 || !StringUtils.hasText(line.getSkuCode())) {
                continue;
            }
            SkuBrief brief = skuBriefMap.get(line.getSkuCode().trim());
            if (brief == null || brief.salePrice == null) {
                continue;
            }
            total = total.add(brief.salePrice.multiply(BigDecimal.valueOf(qty)));
        }
        return total;
    }

    private BigDecimal sumInboundValueFromQuickInboundLogs(Long factoryId) {
        List<EcInventoryLog> logs = ecInventoryLogMapper.selectList(new LambdaQueryWrapper<EcInventoryLog>()
                .eq(EcInventoryLog::getChangeType, CHANGE_INBOUND)
                .and(w -> w.isNull(EcInventoryLog::getRefType)
                        .or()
                        .ne(EcInventoryLog::getRefType, REF_INBOUND_ORDER)));
        if (logs.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<Long> inventoryIds = logs.stream()
                .map(EcInventoryLog::getInventoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, EcInventory> inventoryMap = inventoryIds.isEmpty()
                ? Map.of()
                : listByIds(inventoryIds).stream()
                .collect(Collectors.toMap(EcInventory::getId, inv -> inv, (a, b) -> a));

        Set<String> skuCodes = inventoryMap.values().stream()
                .map(EcInventory::getSkuCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
        Map<String, SkuBrief> skuBriefMap = loadSkuBriefMap(new ArrayList<>(skuCodes));
        Map<String, Long> skuFactoryMap = loadSkuFactoryMap(new ArrayList<>(skuCodes));

        BigDecimal total = BigDecimal.ZERO;
        for (EcInventoryLog log : logs) {
            Long logFactoryId = resolveQuickInboundLogFactoryId(log, inventoryMap, skuFactoryMap);
            if (factoryId != null && !factoryId.equals(logFactoryId)) {
                continue;
            }
            EcInventory inventory = inventoryMap.get(log.getInventoryId());
            if (inventory == null || !StringUtils.hasText(inventory.getSkuCode())) {
                continue;
            }
            SkuBrief brief = skuBriefMap.get(inventory.getSkuCode().trim());
            if (brief == null || brief.salePrice == null) {
                continue;
            }
            int qty = log.getChangeQty() != null ? log.getChangeQty() : 0;
            if (qty <= 0) {
                continue;
            }
            total = total.add(brief.salePrice.multiply(BigDecimal.valueOf(qty)));
        }
        return total;
    }

    private Long resolveQuickInboundLogFactoryId(EcInventoryLog log,
                                                Map<Long, EcInventory> inventoryMap,
                                                Map<String, Long> skuFactoryMap) {
        EcInventory inventory = inventoryMap.get(log.getInventoryId());
        if (inventory == null || !StringUtils.hasText(inventory.getSkuCode())) {
            return null;
        }
        return skuFactoryMap.get(inventory.getSkuCode().trim());
    }

    private Map<String, Long> loadSkuFactoryMap(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return Map.of();
        }
        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .in(EcSku::getSkuCode, skuCodes));
        if (skus.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> productFactoryMap = ecProductMapper.selectBatchIds(
                        skus.stream().map(EcSku::getProductId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(EcProduct::getId, EcProduct::getFactoryId, (a, b) -> a));
        Map<String, Long> result = new HashMap<>();
        for (EcSku sku : skus) {
            result.put(sku.getSkuCode().trim(), productFactoryMap.get(sku.getProductId()));
        }
        return result;
    }

    @Override
    public EcInventoryPackingEstimateVO estimatePacking(String skuCode, Integer outboundQty) {
        if (!StringUtils.hasText(skuCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不能为空");
        }
        SkuContext ctx = loadSkuContext(skuCode.trim());
        if (ctx == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不存在");
        }
        int qty = outboundQty != null && outboundQty > 0 ? outboundQty : 0;
        return buildPackingEstimate(ctx, qty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyStocktake(Long inventoryId, int newQuantity, String refType, Long refId, String remark) {
        EcInventory existing = getById(inventoryId);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (newQuantity < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "盘点后库存不能为负");
        }
        int oldQty = existing.getQuantity() != null ? existing.getQuantity() : 0;
        if (newQuantity == oldQty) {
            return;
        }
        existing.setQuantity(newQuantity);
        updateById(existing);
        insertInventoryLog(inventoryId, CHANGE_STOCKTAKE, Math.abs(newQuantity - oldQty), refType, refId, remark);
    }

    @Override
    public void requireSkuAvailableForInbound(String skuCode) {
        if (!StringUtils.hasText(skuCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择 SKU 货号");
        }
        EcSku sku = ecSkuMapper.selectOne(new LambdaQueryWrapper<EcSku>().eq(EcSku::getSkuCode, skuCode.trim()));
        if (sku == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不存在");
        }
        EcProduct product = ecProductMapper.selectById(sku.getProductId());
        if (!isSkuAvailableForInbound(sku, product)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "货号 " + skuCode + " 已停售或所属商品已禁用，不可进货");
        }
    }

    @Override
    public List<String> listAvailableSkuCodes() {
        Set<String> used = list().stream()
                .map(EcInventory::getSkuCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>().orderByAsc(EcSku::getSkuCode))
                .stream()
                .map(EcSku::getSkuCode)
                .filter(code -> !used.contains(code))
                .toList();
    }

    private Set<String> resolveAllowedSkuCodes(String keyword, Long factoryId) {
        Set<String> allowed = null;

        if (factoryId != null) {
            allowed = new HashSet<>(collectSkuCodesByFactory(factoryId));
        }

        if (StringUtils.hasText(keyword)) {
            Set<String> keywordCodes = collectSkuCodesByKeyword(keyword.trim());
            if (allowed == null) {
                allowed = keywordCodes;
            } else {
                allowed.retainAll(keywordCodes);
            }
        }

        return allowed;
    }

    private Set<String> collectSkuCodesByFactory(Long factoryId) {
        List<EcProduct> products = ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                .eq(EcProduct::getFactoryId, factoryId));
        if (products.isEmpty()) {
            return Set.of();
        }
        Set<Long> productIds = products.stream().map(EcProduct::getId).collect(Collectors.toSet());
        Set<String> skuCodes = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                        .in(EcSku::getProductId, productIds))
                .stream()
                .map(EcSku::getSkuCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (skuCodes.isEmpty()) {
            return Set.of();
        }
        Set<String> inventoryCodes = list().stream()
                .map(EcInventory::getSkuCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        skuCodes.retainAll(inventoryCodes);
        return skuCodes;
    }

    private Set<String> collectSkuCodesByKeyword(String keyword) {
        Set<String> matchedSkuCodes = new HashSet<>();

        list(new LambdaQueryWrapper<EcInventory>().like(EcInventory::getSkuCode, keyword))
                .forEach(item -> matchedSkuCodes.add(item.getSkuCode()));

        ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                        .and(w -> w.like(EcSku::getSkuCode, keyword).or().like(EcSku::getSpecName, keyword)))
                .forEach(sku -> matchedSkuCodes.add(sku.getSkuCode()));

        ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>().like(EcProduct::getName, keyword))
                .forEach(product -> ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                                .eq(EcSku::getProductId, product.getId()))
                        .forEach(sku -> matchedSkuCodes.add(sku.getSkuCode())));

        Set<String> inventoryCodes = list().stream()
                .map(EcInventory::getSkuCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        matchedSkuCodes.retainAll(inventoryCodes);
        return matchedSkuCodes;
    }

    private Map<String, SkuBrief> loadSkuBriefMap(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return Map.of();
        }
        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .in(EcSku::getSkuCode, skuCodes));
        if (skus.isEmpty()) {
            return Map.of();
        }
        Set<Long> productIds = skus.stream().map(EcSku::getProductId).collect(Collectors.toSet());
        Map<Long, String> productNameMap = ecProductMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(EcProduct::getId, EcProduct::getName, (a, b) -> a));

        Map<String, SkuBrief> result = new HashMap<>();
        for (EcSku sku : skus) {
            SkuBrief brief = new SkuBrief();
            brief.specName = sku.getSpecName();
            brief.productName = productNameMap.get(sku.getProductId());
            brief.salePrice = sku.getSalePrice();
            result.put(sku.getSkuCode().trim(), brief);
        }
        return result;
    }

    private EcInventory getOrCreateInventory(String skuCode, Integer alertThreshold, Boolean ignoreAlert) {
        EcInventory inventory = getOne(new LambdaQueryWrapper<EcInventory>()
                .eq(EcInventory::getSkuCode, skuCode));
        if (inventory != null) {
            return inventory;
        }
        inventory = new EcInventory();
        inventory.setSkuCode(skuCode);
        inventory.setQuantity(0);
        inventory.setAlertThreshold(alertThreshold != null ? alertThreshold : ecSystemSettingsService.resolveDefaultAlertThreshold());
        inventory.setIgnoreAlert(Boolean.TRUE.equals(ignoreAlert) ? 1 : 0);
        save(inventory);
        return inventory;
    }

    private void insertInventoryLog(Long inventoryId,
                                    String changeType,
                                    int changeQty,
                                    String refType,
                                    Long refId,
                                    String remark) {
        EcInventoryLog log = new EcInventoryLog();
        log.setInventoryId(inventoryId);
        log.setChangeType(changeType);
        log.setChangeQty(changeQty);
        log.setRefType(refType);
        log.setRefId(refId);
        log.setRemark(trimToNull(remark));
        ecInventoryLogMapper.insert(log);
    }

    private void validateInboundRequest(EcInventoryInboundRequest request) {
        if (request == null || !StringUtils.hasText(request.getSkuCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择 SKU 货号");
        }
        if (request.getAlertThreshold() != null && request.getAlertThreshold() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预警数量不能为负数");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateSaveRequest(EcInventorySaveRequest request, boolean creating) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        if (creating && !StringUtils.hasText(request.getSkuCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择 SKU 货号");
        }
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "库存数量不能为负数");
        }
        if (request.getAlertThreshold() != null && request.getAlertThreshold() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预警数量不能为负数");
        }
    }

    private EcInventory applySaveFields(EcInventorySaveRequest request, EcInventory inventory) {
        if (request.getQuantity() != null) {
            inventory.setQuantity(request.getQuantity());
        } else if (inventory.getQuantity() == null) {
            inventory.setQuantity(0);
        }
        inventory.setIgnoreAlert(Boolean.TRUE.equals(request.getIgnoreAlert()) ? 1 : 0);
        if (request.getAlertThreshold() != null) {
            inventory.setAlertThreshold(request.getAlertThreshold());
        } else if (inventory.getAlertThreshold() == null) {
            inventory.setAlertThreshold(0);
        }
        return inventory;
    }

    private void requireSkuExists(String skuCode) {
        Long count = ecSkuMapper.selectCount(new LambdaQueryWrapper<EcSku>().eq(EcSku::getSkuCode, skuCode));
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不存在");
        }
    }

    private String normalizeChangeType(String changeType) {
        if (!StringUtils.hasText(changeType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择改动方式");
        }
        String normalized = changeType.trim().toUpperCase();
        if (!CHANGE_DEDUCT.equals(normalized) && !CHANGE_RECLAIM.equals(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "改动方式无效");
        }
        return normalized;
    }

    private int requirePositiveQty(Integer qty, String label) {
        if (qty == null || qty <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), label + "必须大于 0");
        }
        return qty;
    }

    private boolean isAlertActive(EcInventory inventory) {
        if (inventory.getIgnoreAlert() != null && inventory.getIgnoreAlert() == 1) {
            return false;
        }
        int threshold = inventory.getAlertThreshold() != null ? inventory.getAlertThreshold() : 0;
        int quantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        return quantity <= threshold;
    }

    private EcInventoryListItemVO toListItemVO(EcInventory inventory, SkuBrief brief, List<EcInventoryLogVO> logs) {
        EcInventoryListItemVO vo = new EcInventoryListItemVO();
        vo.setId(inventory.getId());
        vo.setSkuCode(inventory.getSkuCode());
        if (brief != null) {
            vo.setSpecName(brief.specName);
            vo.setProductName(brief.productName);
            vo.setSalePrice(brief.salePrice);
        }
        vo.setQuantity(inventory.getQuantity());
        vo.setIgnoreAlert(inventory.getIgnoreAlert() != null && inventory.getIgnoreAlert() == 1);
        vo.setAlertThreshold(inventory.getAlertThreshold());
        vo.setAlertActive(isAlertActive(inventory));
        vo.setUpdateTime(inventory.getUpdateTime());
        if (logs.size() > RECENT_LOG_LIMIT) {
            vo.setRecentLogs(logs.subList(0, RECENT_LOG_LIMIT));
        } else {
            vo.setRecentLogs(logs);
        }
        return vo;
    }

    private EcInventoryLogVO toLogVO(EcInventoryLog log) {
        EcInventoryLogVO vo = new EcInventoryLogVO();
        vo.setId(log.getId());
        vo.setInventoryId(log.getInventoryId());
        vo.setChangeType(log.getChangeType());
        vo.setChangeQty(log.getChangeQty());
        vo.setRefType(log.getRefType());
        vo.setRefId(log.getRefId());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private void requireInventoryExists(Long inventoryId) {
        if (getById(inventoryId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    private Set<Long> resolveGlobalLogInventoryIds(String keyword, String skuCode, Long factoryId) {
        Set<String> skuCodes = null;
        if (StringUtils.hasText(skuCode)) {
            skuCodes = Set.of(skuCode.trim());
        }
        Set<String> allowed = resolveAllowedSkuCodes(keyword, factoryId);
        if (allowed != null) {
            if (skuCodes == null) {
                skuCodes = allowed;
            } else {
                skuCodes.retainAll(allowed);
            }
        }
        if (skuCodes == null) {
            return null;
        }
        if (skuCodes.isEmpty()) {
            return Set.of();
        }
        return list(new LambdaQueryWrapper<EcInventory>().in(EcInventory::getSkuCode, skuCodes))
                .stream()
                .map(EcInventory::getId)
                .collect(Collectors.toSet());
    }

    private EcInventoryGlobalLogVO toGlobalLogVO(EcInventoryLog log, EcInventory inventory, SkuContext ctx) {
        EcInventoryGlobalLogVO vo = new EcInventoryGlobalLogVO();
        vo.setId(log.getId());
        vo.setInventoryId(log.getInventoryId());
        vo.setChangeType(log.getChangeType());
        vo.setChangeQty(log.getChangeQty());
        vo.setRefType(log.getRefType());
        vo.setRefId(log.getRefId());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        if (inventory != null) {
            vo.setSkuCode(inventory.getSkuCode());
        }
        if (ctx != null) {
            vo.setSpecName(ctx.specName);
            vo.setProductName(ctx.productName);
            vo.setFactoryId(ctx.factoryId);
            vo.setFactoryName(ctx.factoryName);
        }
        return vo;
    }

    private EcInventoryDetailVO toDetailVO(EcInventory inventory, SkuContext ctx, List<EcInventoryLogVO> recentLogs) {
        EcInventoryDetailVO vo = new EcInventoryDetailVO();
        vo.setId(inventory.getId());
        vo.setSkuCode(inventory.getSkuCode());
        if (ctx != null) {
            vo.setSpecName(ctx.specName);
            vo.setProductName(ctx.productName);
            vo.setSalePrice(ctx.salePrice);
            vo.setSkuId(ctx.skuId);
            vo.setProductId(ctx.productId);
            vo.setFactoryId(ctx.factoryId);
            vo.setFactoryName(ctx.factoryName);
            vo.setSkuStatus(ctx.skuStatus);
        }
        vo.setQuantity(inventory.getQuantity());
        vo.setIgnoreAlert(inventory.getIgnoreAlert() != null && inventory.getIgnoreAlert() == 1);
        vo.setAlertThreshold(inventory.getAlertThreshold());
        vo.setAlertActive(isAlertActive(inventory));
        vo.setUpdateTime(inventory.getUpdateTime());
        vo.setRecentLogs(recentLogs);
        vo.setInTransitQty(loadInTransitQty(inventory.getSkuCode()));
        vo.setRelatedInboundOrders(loadRelatedInboundOrders(inventory.getSkuCode()));
        vo.setRelatedOutboundOrders(loadRelatedOutboundOrders(inventory.getSkuCode()));
        int qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        if (ctx != null) {
            vo.setImageName(ctx.imageName);
            vo.setPackingEstimate(buildPackingEstimate(ctx, qty));
            vo.setOutboundPackingEstimate(buildPackingEstimate(ctx, qty));
        }
        return vo;
    }

    private int loadInTransitQty(String skuCode) {
        return loadInTransitMap(List.of(skuCode)).getOrDefault(skuCode, 0);
    }

    private Map<String, Integer> loadInTransitMap(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return Map.of();
        }
        List<EcInboundOrder> draftOrders = ecInboundOrderMapper.selectList(
                new LambdaQueryWrapper<EcInboundOrder>().eq(EcInboundOrder::getStatus, STATUS_DRAFT));
        if (draftOrders.isEmpty()) {
            return skuCodes.stream().collect(Collectors.toMap(code -> code, code -> 0, (a, b) -> a));
        }
        Set<Long> orderIds = draftOrders.stream().map(EcInboundOrder::getId).collect(Collectors.toSet());
        List<EcInboundOrderLine> lines = ecInboundOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcInboundOrderLine>()
                        .in(EcInboundOrderLine::getOrderId, orderIds)
                        .in(EcInboundOrderLine::getSkuCode, skuCodes));
        Map<String, Integer> result = new HashMap<>();
        for (String skuCode : skuCodes) {
            result.put(skuCode, 0);
        }
        for (EcInboundOrderLine line : lines) {
            result.merge(line.getSkuCode(),
                    line.getQuantity() != null ? line.getQuantity() : 0,
                    Integer::sum);
        }
        return result;
    }

    private List<EcInventoryInboundBriefVO> loadRelatedInboundOrders(String skuCode) {
        List<EcInboundOrderLine> lines = ecInboundOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcInboundOrderLine>()
                        .eq(EcInboundOrderLine::getSkuCode, skuCode)
                        .orderByDesc(EcInboundOrderLine::getId));
        if (lines.isEmpty()) {
            return List.of();
        }
        Set<Long> orderIds = lines.stream().map(EcInboundOrderLine::getOrderId).collect(Collectors.toSet());
        Map<Long, EcInboundOrder> orderMap = ecInboundOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(EcInboundOrder::getId, o -> o, (a, b) -> a));

        List<EcInventoryInboundBriefVO> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (EcInboundOrderLine line : lines) {
            if (!seen.add(line.getOrderId())) {
                continue;
            }
            EcInboundOrder order = orderMap.get(line.getOrderId());
            if (order == null) {
                continue;
            }
            EcInventoryInboundBriefVO brief = new EcInventoryInboundBriefVO();
            brief.setId(order.getId());
            brief.setOrderNo(order.getOrderNo());
            brief.setStatus(order.getStatus());
            brief.setQuantity(line.getQuantity());
            brief.setReceivedQuantity(line.getReceivedQuantity());
            brief.setOrderTime(order.getOrderTime());
            brief.setExpectedDeliveryTime(order.getExpectedDeliveryTime());
            brief.setActualReceiptTime(order.getActualReceiptTime());
            result.add(brief);
        }
        result.sort(Comparator.comparing(EcInventoryInboundBriefVO::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private List<EcInventoryOutboundBriefVO> loadRelatedOutboundOrders(String skuCode) {
        List<EcOutboundOrderLine> lines = ecOutboundOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcOutboundOrderLine>()
                        .eq(EcOutboundOrderLine::getSkuCode, skuCode)
                        .orderByDesc(EcOutboundOrderLine::getId));
        if (lines.isEmpty()) {
            return List.of();
        }
        Set<Long> orderIds = lines.stream().map(EcOutboundOrderLine::getOrderId).collect(Collectors.toSet());
        Map<Long, EcOutboundOrder> orderMap = ecOutboundOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(EcOutboundOrder::getId, o -> o, (a, b) -> a));

        List<EcInventoryOutboundBriefVO> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (EcOutboundOrderLine line : lines) {
            if (!seen.add(line.getOrderId())) {
                continue;
            }
            EcOutboundOrder order = orderMap.get(line.getOrderId());
            if (order == null) {
                continue;
            }
            EcInventoryOutboundBriefVO brief = new EcInventoryOutboundBriefVO();
            brief.setId(order.getId());
            brief.setOrderNo(order.getOrderNo());
            brief.setStatus(order.getStatus());
            brief.setQuantity(line.getQuantity());
            brief.setShippedQuantity(line.getShippedQuantity());
            brief.setOrderTime(order.getOrderTime());
            brief.setExpectedShipTime(order.getExpectedShipTime());
            brief.setActualShipTime(order.getActualShipTime());
            result.add(brief);
        }
        result.sort(Comparator.comparing(EcInventoryOutboundBriefVO::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private SkuContext loadSkuContext(String skuCode) {
        Map<String, SkuContext> map = loadSkuContextMap(List.of(skuCode));
        return map.get(skuCode);
    }

    private Map<String, SkuContext> loadSkuContextMap(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return Map.of();
        }
        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .in(EcSku::getSkuCode, skuCodes));
        if (skus.isEmpty()) {
            return Map.of();
        }
        Set<Long> productIds = skus.stream().map(EcSku::getProductId).collect(Collectors.toSet());
        Map<Long, EcProduct> productMap = ecProductMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(EcProduct::getId, p -> p, (a, b) -> a));
        Set<Long> factoryIds = productMap.values().stream()
                .map(EcProduct::getFactoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> factoryNameMap = factoryIds.isEmpty() ? Map.of()
                : ecFactoryMapper.selectBatchIds(factoryIds).stream()
                .collect(Collectors.toMap(EcFactory::getId, EcFactory::getName, (a, b) -> a));

        Set<Long> cartonIds = skus.stream()
                .map(EcSku::getCartonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, EcCarton> cartonMap = cartonIds.isEmpty() ? Map.of()
                : ecCartonMapper.selectBatchIds(cartonIds).stream()
                .collect(Collectors.toMap(EcCarton::getId, c -> c, (a, b) -> a));

        Map<String, SkuContext> result = new HashMap<>();
        for (EcSku sku : skus) {
            SkuContext ctx = new SkuContext();
            ctx.skuId = sku.getId();
            ctx.productId = sku.getProductId();
            ctx.specName = sku.getSpecName();
            ctx.salePrice = sku.getSalePrice();
            ctx.skuStatus = sku.getStatus();
            if (StringUtils.hasText(sku.getImageName())) {
                ctx.imageName = sku.getImageName().trim();
            }
            ctx.unitsPerCarton = sku.getUnitsPerCarton();
            ctx.cartonId = sku.getCartonId();
            EcProduct product = productMap.get(sku.getProductId());
            if (product != null) {
                ctx.productName = product.getName();
                ctx.productStatus = product.getStatus();
                ctx.factoryId = product.getFactoryId();
                if (product.getFactoryId() != null) {
                    ctx.factoryName = factoryNameMap.get(product.getFactoryId());
                }
                if (!StringUtils.hasText(ctx.imageName) && StringUtils.hasText(product.getImageName())) {
                    ctx.imageName = product.getImageName().trim();
                }
            }
            if (sku.getCartonId() != null) {
                EcCarton carton = cartonMap.get(sku.getCartonId());
                if (carton != null) {
                    ctx.cartonName = carton.getName();
                    ctx.cartonLengthCm = carton.getLengthCm();
                    ctx.cartonWidthCm = carton.getWidthCm();
                    ctx.cartonHeightCm = carton.getHeightCm();
                }
            }
            result.put(sku.getSkuCode(), ctx);
        }
        return result;
    }

    private EcInventoryPackingEstimateVO buildPackingEstimate(SkuContext ctx, int outboundQty) {
        EcInventoryPackingEstimateVO vo = new EcInventoryPackingEstimateVO();
        vo.setOutboundQty(outboundQty);
        int unitsPerCarton = ctx.unitsPerCarton != null && ctx.unitsPerCarton > 0 ? ctx.unitsPerCarton : 1;
        vo.setUnitsPerCarton(unitsPerCarton);
        vo.setCartonId(ctx.cartonId);
        vo.setCartonName(ctx.cartonName);
        if (outboundQty <= 0) {
            vo.setCartonsNeeded(0);
            vo.setCartonVolumeCm3(calcVolume(ctx.cartonLengthCm, ctx.cartonWidthCm, ctx.cartonHeightCm));
            vo.setTotalVolumeCm3(BigDecimal.ZERO);
            return vo;
        }
        int cartonsNeeded = (outboundQty + unitsPerCarton - 1) / unitsPerCarton;
        vo.setCartonsNeeded(cartonsNeeded);
        BigDecimal cartonVolume = calcVolume(ctx.cartonLengthCm, ctx.cartonWidthCm, ctx.cartonHeightCm);
        vo.setCartonVolumeCm3(cartonVolume);
        if (cartonVolume != null) {
            vo.setTotalVolumeCm3(cartonVolume.multiply(BigDecimal.valueOf(cartonsNeeded)));
        }
        return vo;
    }

    private BigDecimal calcVolume(BigDecimal length, BigDecimal width, BigDecimal height) {
        if (length == null || width == null || height == null) {
            return null;
        }
        return length.multiply(width).multiply(height).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isSkuAvailableForInbound(EcSku sku, EcProduct product) {
        if (sku == null) {
            return false;
        }
        if (!SKU_ON_SALE.equals(sku.getStatus())) {
            return false;
        }
        if (product == null) {
            return false;
        }
        return PRODUCT_ENABLED.equals(product.getStatus());
    }

    private static final class SkuBrief {
        private String specName;
        private String productName;
        private java.math.BigDecimal salePrice;
    }

    private static final class SkuContext {
        private Long skuId;
        private Long productId;
        private Long factoryId;
        private String factoryName;
        private String specName;
        private String productName;
        private String skuStatus;
        private String productStatus;
        private java.math.BigDecimal salePrice;
        private String imageName;
        private Integer unitsPerCarton;
        private Long cartonId;
        private String cartonName;
        private java.math.BigDecimal cartonLengthCm;
        private java.math.BigDecimal cartonWidthCm;
        private java.math.BigDecimal cartonHeightCm;
    }
}
