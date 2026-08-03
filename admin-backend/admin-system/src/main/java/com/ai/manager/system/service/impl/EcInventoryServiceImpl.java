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
import com.ai.manager.system.domain.vo.EcInventoryOverviewVO;
import com.ai.manager.system.domain.vo.EcInventoryPackingEstimateVO;
import com.ai.manager.system.domain.vo.EcInventorySummaryVO;
import com.ai.manager.system.domain.vo.EcInventorySkuOptionVO;
import com.ai.manager.system.domain.vo.EcInventorySpuStatusVO;
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
import com.ai.manager.system.service.support.EcInventoryVoAssembler;
import com.ai.manager.system.service.support.EcInventoryVoAssembler.SkuBrief;
import com.ai.manager.system.service.support.EcInventoryContextLoader;
import com.ai.manager.system.service.support.EcInventoryVoAssembler.SkuContext;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcInventoryServiceImpl extends ServiceImpl<EcInventoryMapper, EcInventory> implements EcInventoryService {

    private static final String CHANGE_DEDUCT = "DEDUCT";
    private static final String CHANGE_RECLAIM = "RECLAIM";
    private static final String CHANGE_INBOUND = "INBOUND";
    private static final String CHANGE_STOCKTAKE = "STOCKTAKE";
    private static final String REF_INBOUND_ORDER = "INBOUND_ORDER";
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
    private final EcInventoryVoAssembler inventoryVoAssembler;
    private final EcInventoryContextLoader inventoryContextLoader;

    @Override
    public PageResult<EcInventoryListItemVO> pageInventories(String keyword, Boolean alertOnly, Boolean inStockOnly,
                                                               Long factoryId, Long page, Long pageSize,
                                                               Boolean groupBySpu) {
        if (Boolean.TRUE.equals(groupBySpu)) {
            return pageInventoriesGroupedBySpu(keyword, alertOnly, inStockOnly, factoryId, page, pageSize);
        }
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

        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(
                entityPage.getRecords().stream().map(EcInventory::getSkuCode).toList());
        Map<String, Integer> inTransitMap = inventoryContextLoader.loadInTransitMap(
                entityPage.getRecords().stream().map(EcInventory::getSkuCode).toList());
        List<EcInventoryListItemVO> records = new ArrayList<>();
        for (EcInventory inventory : entityPage.getRecords()) {
            EcInventoryListItemVO item = inventoryVoAssembler.toListItemVO(
                    inventory, skuBriefMap.get(inventory.getSkuCode()), List.of());
            item.setInTransitQty(inTransitMap.getOrDefault(inventory.getSkuCode(), 0));
            records.add(item);
        }
        PageResult<EcInventoryListItemVO> result = PageUtils.of(
                records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        result.setExtra(extra);
        return result;
    }

    @Override
    public EcInventoryOverviewVO getInventoryOverview() {
        List<EcInventory> all = list();
        int skuCount = all.size();
        long totalQuantity = 0;
        long totalStockValue = 0;
        int normal = 0, low = 0, zero = 0;

        // 加载 SKU 价格信息（EcInventory 表不含 salePrice，需从 ec_sku 关联获取）
        List<String> allSkuCodes = all.stream()
                .map(EcInventory::getSkuCode)
                .collect(Collectors.toList());
        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(allSkuCodes);

        for (EcInventory inv : all) {
            int qty = inv.getQuantity() != null ? inv.getQuantity() : 0;
            totalQuantity += qty;

            // 库存价值
            SkuBrief brief = skuBriefMap.get(inv.getSkuCode());
            if (brief != null && brief.salePrice != null) {
                totalStockValue += (long) (qty * brief.salePrice.doubleValue());
            }

            // 状态分类
            if (qty <= 0) {
                zero++;
            } else if (inventoryVoAssembler.isAlertActive(inv)) {
                low++;
            } else {
                normal++;
            }
        }

        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("normal", normal);
        statusCounts.put("low", low);
        statusCounts.put("zero", zero);

        EcInventoryOverviewVO vo = new EcInventoryOverviewVO();
        vo.setSkuCount(skuCount);
        vo.setTotalQuantity((int) totalQuantity);
        vo.setTotalStockValue((int) totalStockValue);
        vo.setStatusCounts(statusCounts);
        return vo;
    }

    @Override
    public EcInventorySummaryVO getInventorySummary(Long factoryId) {
        // 按工厂过滤
        LambdaQueryWrapper<EcInventory> wrapper = new LambdaQueryWrapper<>();
        if (factoryId != null) {
            Set<String> allowed = resolveAllowedSkuCodes(null, factoryId);
            if (allowed != null && allowed.isEmpty()) {
                return emptySummary();
            }
            if (allowed != null) {
                wrapper.in(EcInventory::getSkuCode, allowed);
            }
        }
        List<EcInventory> all = list(wrapper);
        if (all.isEmpty()) {
            return emptySummary();
        }

        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(
                all.stream().map(EcInventory::getSkuCode).distinct().toList());

        int skuCount = all.size();
        long totalQuantity = 0;
        long totalStockValue = 0;
        int alertCount = 0;
        int normal = 0, low = 0, zero = 0;

        for (EcInventory inv : all) {
            int qty = inv.getQuantity() != null ? inv.getQuantity() : 0;
            totalQuantity += qty;

            // 库存价值
            SkuBrief brief = skuBriefMap.get(inv.getSkuCode());
            if (brief != null && brief.salePrice != null) {
                totalStockValue += (long) (qty * brief.salePrice.doubleValue());
            }

            // 状态分类 & 预警计数
            if (qty <= 0) {
                zero++;
            } else if (inventoryVoAssembler.isAlertActive(inv)) {
                low++;
                alertCount++;
            } else {
                normal++;
            }
        }

        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("normal", normal);
        statusCounts.put("low", low);
        statusCounts.put("zero", zero);

        // 健康度评分（与前端 computeInventoryHealthScore 口径一致）
        int healthScore = 100;
        if (skuCount > 0) {
            healthScore = Math.round((normal * 100f + low * 55f) / skuCount);
        }

        // 入库货值
        BigDecimal inboundValue = BigDecimal.ZERO;
        try {
            inboundValue = summarizeHistoricalInboundValue(factoryId).getTotalInboundValue();
        } catch (Exception ignored) {
            // 孤立异常不影响主数据
        }

        EcInventorySummaryVO vo = new EcInventorySummaryVO();
        vo.setSkuCount(skuCount);
        vo.setTotalQuantity((int) totalQuantity);
        vo.setTotalStockValue(totalStockValue);
        vo.setAlertCount(alertCount);
        vo.setStatusCounts(statusCounts);
        vo.setHealthScore(healthScore);
        vo.setInboundValue(inboundValue);
        return vo;
    }

    private EcInventorySummaryVO emptySummary() {
        EcInventorySummaryVO vo = new EcInventorySummaryVO();
        vo.setSkuCount(0);
        vo.setTotalQuantity(0);
        vo.setTotalStockValue(0);
        vo.setAlertCount(0);
        Map<String, Integer> emptyMap = new HashMap<>();
        emptyMap.put("normal", 0);
        emptyMap.put("low", 0);
        emptyMap.put("zero", 0);
        vo.setStatusCounts(emptyMap);
        vo.setHealthScore(100);
        vo.setInboundValue(BigDecimal.ZERO);
        return vo;
    }

    @Override
    public EcInventorySpuStatusVO getSpuStatusCounts() {
        List<EcInventory> all = list();
        List<String> allSkuCodes = all.stream().map(EcInventory::getSkuCode).distinct().toList();
        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(allSkuCodes);

        // 按 productId 分组
        Map<String, List<EcInventory>> spuGroups = new LinkedHashMap<>();
        for (EcInventory inv : all) {
            SkuBrief brief = skuBriefMap.get(inv.getSkuCode());
            String groupKey = brief != null && brief.productId != null
                    ? "p:" + brief.productId
                    : "s:" + inv.getSkuCode();
            spuGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(inv);
        }

        int normal = 0, low = 0, zero = 0;
        for (Map.Entry<String, List<EcInventory>> entry : spuGroups.entrySet()) {
            List<EcInventory> groupItems = entry.getValue();
            boolean allZero = groupItems.stream().allMatch(i -> (i.getQuantity() != null ? i.getQuantity() : 0) <= 0);
            boolean hasAlert = groupItems.stream().anyMatch(inventoryVoAssembler::isAlertActive);

            if (allZero) {
                zero++;
            } else if (hasAlert) {
                low++;
            } else {
                normal++;
            }
        }

        EcInventorySpuStatusVO vo = new EcInventorySpuStatusVO();
        vo.setTotal(spuGroups.size());
        vo.setNormal(normal);
        vo.setLow(low);
        vo.setZero(zero);
        return vo;
    }

    @Override
    public List<EcInventoryListItemVO> getInventoryByProduct(Long productId) {
        if (productId == null) return List.of();
        List<EcSku> skus = ecSkuMapper.selectList(
                new LambdaQueryWrapper<EcSku>().eq(EcSku::getProductId, productId));
        if (skus.isEmpty()) return List.of();

        List<String> skuCodes = skus.stream().map(EcSku::getSkuCode).toList();
        List<EcInventory> inventories = list(new LambdaQueryWrapper<EcInventory>()
                .in(EcInventory::getSkuCode, skuCodes)
                .orderByAsc(EcInventory::getSkuCode));

        if (inventories.isEmpty()) return List.of();

        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(
                inventories.stream().map(EcInventory::getSkuCode).toList());
        Map<String, Integer> inTransitMap = inventoryContextLoader.loadInTransitMap(
                inventories.stream().map(EcInventory::getSkuCode).toList());

        List<EcInventoryListItemVO> list = new ArrayList<>();
        for (EcInventory inv : inventories) {
            SkuBrief brief = skuBriefMap.get(inv.getSkuCode());
            EcInventoryListItemVO item = inventoryVoAssembler.toListItemVO(inv, brief, List.of());
            item.setInTransitQty(inTransitMap.getOrDefault(inv.getSkuCode(), 0));
            list.add(item);
        }
        return list;
    }

    private PageResult<EcInventoryListItemVO> pageInventoriesGroupedBySpu(
            String keyword, Boolean alertOnly, Boolean inStockOnly,
            Long factoryId, Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);

        LambdaQueryWrapper<EcInventory> wrapper = buildInventoryQueryWrapper(keyword, factoryId, alertOnly, inStockOnly);
        Set<String> allowedSkuCodes = resolveAllowedSkuCodes(keyword, factoryId);
        if (allowedSkuCodes != null && allowedSkuCodes.isEmpty()) {
            return PageResult.empty(p, ps);
        }

        // 获取所有匹配的 SKU 级库存
        List<EcInventory> allInventories = list(wrapper);
        if (allInventories.isEmpty()) {
            return PageResult.empty(p, ps);
        }

        // 加载 SKU 信息（含 productId/productName）
        List<String> allSkuCodes = allInventories.stream().map(EcInventory::getSkuCode).distinct().toList();
        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(allSkuCodes);
        Map<String, Integer> inTransitMap = inventoryContextLoader.loadInTransitMap(allSkuCodes);

        // 按 productId 分组（无 productId 的用 skuCode 作为独立组）
        Map<String, List<EcInventory>> spuGroups = new LinkedHashMap<>();
        for (EcInventory inv : allInventories) {
            SkuBrief brief = skuBriefMap.get(inv.getSkuCode());
            String groupKey = brief != null && brief.productId != null
                    ? "p:" + brief.productId
                    : "s:" + inv.getSkuCode();
            spuGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(inv);
        }

        // 构建 SPU 级记录
        List<EcInventoryListItemVO> spuRecords = new ArrayList<>();
        for (Map.Entry<String, List<EcInventory>> entry : spuGroups.entrySet()) {
            List<EcInventory> groupItems = entry.getValue();
            EcInventory first = groupItems.get(0);
            SkuBrief brief = skuBriefMap.get(first.getSkuCode());

            int totalQty = groupItems.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
            int totalInTransit = groupItems.stream()
                    .mapToInt(i -> inTransitMap.getOrDefault(i.getSkuCode(), 0)).sum();
            int totalThreshold = groupItems.stream().mapToInt(i -> i.getAlertThreshold() != null ? i.getAlertThreshold() : 0).sum();
            boolean hasAlert = groupItems.stream().anyMatch(inventoryVoAssembler::isAlertActive);
            boolean anyIgnoreAlert = groupItems.stream().anyMatch(i -> i.getIgnoreAlert() != null && i.getIgnoreAlert() == 1);

            EcInventoryListItemVO vo = new EcInventoryListItemVO();
            vo.setId(first.getId());
            vo.setSkuCode(brief != null && brief.productName != null ? brief.productName : first.getSkuCode());
            vo.setProductName(brief != null ? brief.productName : null);
            vo.setProductId(brief != null ? brief.productId : null);
            vo.setQuantity(totalQty);
            vo.setInTransitQty(totalInTransit);
            vo.setAlertThreshold(totalThreshold);
            vo.setAlertActive(hasAlert);
            vo.setIgnoreAlert(anyIgnoreAlert);
            vo.setSalePrice(brief != null ? brief.salePrice : null);
            vo.setUpdateTime(groupItems.stream()
                    .map(EcInventory::getUpdateTime)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null));
            vo.setSpuSkuCount(groupItems.size());
            spuRecords.add(vo);
        }

        // SPU 级排序：按数量降序
        spuRecords.sort((a, b) -> Integer.compare(
                b.getQuantity() != null ? b.getQuantity() : 0,
                a.getQuantity() != null ? a.getQuantity() : 0));

        // 内存分页
        int totalSpu = spuRecords.size();
        int fromIndex = (int) ((p - 1) * ps);
        if (fromIndex >= totalSpu) {
            return PageUtils.of(List.of(), (long) totalSpu, p, ps);
        }
        int toIndex = Math.min(fromIndex + (int) ps, totalSpu);
        List<EcInventoryListItemVO> pageRecords = spuRecords.subList(fromIndex, toIndex);

        // extra 汇总
        Map<String, Object> extra = new HashMap<>();
        extra.put("totalSkuCount", allInventories.size());
        extra.put("totalQuantity", spuRecords.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum());
        extra.put("totalStockValue", spuRecords.stream()
                .map(i -> {
                    BigDecimal qty = BigDecimal.valueOf(i.getQuantity() != null ? i.getQuantity() : 0);
                    return i.getSalePrice() != null ? i.getSalePrice().multiply(qty) : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        PageResult<EcInventoryListItemVO> result = PageUtils.of(pageRecords, (long) totalSpu, p, ps);
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
        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(
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
        SkuContext skuCtx = inventoryContextLoader.loadSkuContext(inventory.getSkuCode());
        List<EcInventoryLogVO> recentLogs = listLogs(id);
        if (recentLogs.size() > RECENT_LOG_LIMIT) {
            recentLogs = recentLogs.subList(0, RECENT_LOG_LIMIT);
        }
        return inventoryVoAssembler.toDetailVO(inventory, skuCtx, recentLogs,
                inventoryContextLoader.loadInTransitQty(inventory.getSkuCode()),
                inventoryContextLoader.loadRelatedInboundOrders(inventory.getSkuCode()),
                inventoryContextLoader.loadRelatedOutboundOrders(inventory.getSkuCode()));
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
        return inventoryVoAssembler.toListItemVO(existing, inventoryContextLoader.loadSkuBriefMap(List.of(existing.getSkuCode())).get(existing.getSkuCode()), List.of());
    }

    private EcInventoryListItemVO getInventoryListItem(Long id) {
        EcInventory inventory = getById(id);
        if (inventory == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(List.of(inventory.getSkuCode()));
        return inventoryVoAssembler.toListItemVO(inventory, skuBriefMap.get(inventory.getSkuCode()), List.of());
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
            vo.setInboundAllowed(inventoryVoAssembler.isSkuAvailableForInbound(sku, product));
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
                .map(inventoryVoAssembler::toLogVO)
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
                .map(inventoryVoAssembler::toLogVO)
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
        Map<String, SkuContext> skuContextMap = inventoryContextLoader.loadSkuContextMap(skuCodes);

        List<EcInventoryGlobalLogVO> records = new ArrayList<>();
        for (EcInventoryLog log : entityPage.getRecords()) {
            EcInventory inventory = inventoryMap.get(log.getInventoryId());
            SkuContext ctx = inventory != null ? skuContextMap.get(inventory.getSkuCode()) : null;
            records.add(inventoryVoAssembler.toGlobalLogVO(log, inventory, ctx));
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
                    if (inventoryVoAssembler.isAlertActive(inventory)) {
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
        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(new ArrayList<>(skuCodes));

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
        Map<String, SkuBrief> skuBriefMap = inventoryContextLoader.loadSkuBriefMap(new ArrayList<>(skuCodes));
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
        SkuContext ctx = inventoryContextLoader.loadSkuContext(skuCode.trim());
        if (ctx == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不存在");
        }
        int qty = outboundQty != null && outboundQty > 0 ? outboundQty : 0;
        return inventoryVoAssembler.buildPackingEstimate(ctx, qty);
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
        if (!inventoryVoAssembler.isSkuAvailableForInbound(sku, product)) {
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




}
