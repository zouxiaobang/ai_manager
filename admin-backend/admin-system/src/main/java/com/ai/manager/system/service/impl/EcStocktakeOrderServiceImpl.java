package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcStocktakeOrderLineItem;
import com.ai.manager.system.domain.dto.EcStocktakeOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcInventory;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.entity.EcStocktakeOrder;
import com.ai.manager.system.domain.entity.EcStocktakeOrderLine;
import com.ai.manager.system.domain.vo.EcStocktakeOrderDetailVO;
import com.ai.manager.system.domain.vo.EcStocktakeOrderLineVO;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcInventoryMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.mapper.EcStocktakeOrderLineMapper;
import com.ai.manager.system.mapper.EcStocktakeOrderMapper;
import com.ai.manager.system.service.EcInventoryService;
import com.ai.manager.system.service.EcStocktakeOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EcStocktakeOrderServiceImpl extends ServiceImpl<EcStocktakeOrderMapper, EcStocktakeOrder>
        implements EcStocktakeOrderService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String REF_STOCKTAKE_ORDER = "STOCKTAKE_ORDER";

    private final EcStocktakeOrderLineMapper ecStocktakeOrderLineMapper;
    private final EcInventoryService ecInventoryService;
    private final EcInventoryMapper ecInventoryMapper;
    private final EcSkuMapper ecSkuMapper;
    private final EcProductMapper ecProductMapper;
    private final EcFactoryMapper ecFactoryMapper;

    @Override
    public PageResult<EcStocktakeOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                           Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcStocktakeOrder> wrapper = new LambdaQueryWrapper<EcStocktakeOrder>()
                .orderByDesc(EcStocktakeOrder::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(EcStocktakeOrder::getStatus, status.trim().toUpperCase());
        }
        if (factoryId != null) {
            wrapper.eq(EcStocktakeOrder::getFactoryId, factoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcStocktakeOrder::getOrderNo, kw).or().like(EcStocktakeOrder::getRemark, kw));
        }
        Page<EcStocktakeOrder> entityPage = page(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            return PageUtils.of(List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(entityPage.getRecords().stream()
                .map(EcStocktakeOrder::getFactoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        List<EcStocktakeOrderDetailVO> records = new ArrayList<>();
        for (EcStocktakeOrder order : entityPage.getRecords()) {
            records.add(toDetailVO(order, loadLines(order.getId()), factoryNameMap));
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public EcStocktakeOrderDetailVO getOrderDetail(Long id) {
        EcStocktakeOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(
                order.getFactoryId() == null ? List.of() : List.of(order.getFactoryId()));
        return toDetailVO(order, loadLines(id), factoryNameMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcStocktakeOrderDetailVO createOrder(EcStocktakeOrderSaveRequest request) {
        List<NormalizedLine> lines = validateAndNormalizeLines(request, null);
        EcStocktakeOrder order = new EcStocktakeOrder();
        order.setOrderNo(generateOrderNo());
        order.setFactoryId(request.getFactoryId());
        order.setStatus(STATUS_DRAFT);
        order.setRemark(trimToNull(request.getRemark()));
        order.setStocktakeTime(requireStocktakeTime(request.getStocktakeTime()));
        save(order);
        saveLines(order.getId(), lines);
        return getOrderDetail(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcStocktakeOrderDetailVO updateOrder(Long id, EcStocktakeOrderSaveRequest request) {
        EcStocktakeOrder order = requireDraftOrder(id);
        List<NormalizedLine> lines = validateAndNormalizeLines(request, order.getFactoryId());
        order.setFactoryId(request.getFactoryId());
        order.setRemark(trimToNull(request.getRemark()));
        order.setStocktakeTime(requireStocktakeTime(request.getStocktakeTime()));
        updateById(order);
        ecStocktakeOrderLineMapper.delete(new LambdaQueryWrapper<EcStocktakeOrderLine>()
                .eq(EcStocktakeOrderLine::getOrderId, id));
        saveLines(id, lines);
        return getOrderDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcStocktakeOrderDetailVO confirmOrder(Long id) {
        EcStocktakeOrder order = requireDraftOrder(id);
        List<EcStocktakeOrderLine> lines = ecStocktakeOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcStocktakeOrderLine>()
                        .eq(EcStocktakeOrderLine::getOrderId, id)
                        .orderByAsc(EcStocktakeOrderLine::getId));
        if (lines.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "盘点单至少包含一条明细");
        }
        validateLinesForFactory(order.getFactoryId(), lines);

        boolean hasAdjustment = false;
        String remarkPrefix = "盘点单 " + order.getOrderNo();
        for (EcStocktakeOrderLine line : lines) {
            if (line.getActualQuantity() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "货号 " + line.getSkuCode() + " 未填写实盘数量");
            }
            EcInventory inventory = ecInventoryMapper.selectOne(new LambdaQueryWrapper<EcInventory>()
                    .eq(EcInventory::getSkuCode, line.getSkuCode()));
            if (inventory == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "货号 " + line.getSkuCode() + " 尚无库存记录");
            }
            int oldQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            int actualQty = line.getActualQuantity();
            if (actualQty == oldQty) {
                continue;
            }
            hasAdjustment = true;
            String lineRemark = remarkPrefix + "（账面" + line.getBookQuantity() + "，实盘" + actualQty + "）";
            ecInventoryService.applyStocktake(inventory.getId(), actualQty, REF_STOCKTAKE_ORDER, order.getId(), lineRemark);
        }
        if (!hasAdjustment) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "盘点结果与账面一致，无需确认");
        }

        order.setStatus(STATUS_CONFIRMED);
        updateById(order);
        return getOrderDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        EcStocktakeOrder order = requireDraftOrder(id);
        order.setStatus(STATUS_CANCELLED);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        EcStocktakeOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅草稿盘点单可删除");
        }
        ecStocktakeOrderLineMapper.delete(new LambdaQueryWrapper<EcStocktakeOrderLine>()
                .eq(EcStocktakeOrderLine::getOrderId, id));
        removeById(id);
    }

    private EcStocktakeOrder requireDraftOrder(Long id) {
        EcStocktakeOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅草稿盘点单可编辑或确认");
        }
        return order;
    }

    private List<NormalizedLine> validateAndNormalizeLines(EcStocktakeOrderSaveRequest request,
                                                           Long existingFactoryId) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "盘点单至少包含一条明细");
        }
        Long factoryId = request.getFactoryId() != null ? request.getFactoryId() : existingFactoryId;
        if (factoryId != null) {
            EcFactory factory = ecFactoryMapper.selectById(factoryId);
            if (factory == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工厂不存在");
            }
        }

        List<NormalizedLine> normalized = new ArrayList<>();
        Set<String> skuCodes = new HashSet<>();
        for (EcStocktakeOrderLineItem line : request.getLines()) {
            if (line == null || !StringUtils.hasText(line.getSkuCode())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细货号不能为空");
            }
            String skuCode = line.getSkuCode().trim();
            if (!skuCodes.add(skuCode)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "盘点单存在重复货号：" + skuCode);
            }
            EcSku sku = ecSkuMapper.selectOne(new LambdaQueryWrapper<EcSku>().eq(EcSku::getSkuCode, skuCode));
            if (sku == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不存在：" + skuCode);
            }
            if (factoryId != null) {
                EcProduct product = ecProductMapper.selectById(sku.getProductId());
                if (product == null || !factoryId.equals(product.getFactoryId())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                            "货号 " + skuCode + " 不属于所选工厂");
                }
            }
            EcInventory inventory = ecInventoryMapper.selectOne(new LambdaQueryWrapper<EcInventory>()
                    .eq(EcInventory::getSkuCode, skuCode));
            if (inventory == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "货号 " + skuCode + " 尚无库存记录，请先建立库存");
            }
            if (line.getActualQuantity() != null && line.getActualQuantity() < 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "货号 " + skuCode + " 实盘数量不能为负");
            }
            NormalizedLine item = new NormalizedLine();
            item.skuCode = skuCode;
            item.bookQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            item.actualQuantity = line.getActualQuantity();
            normalized.add(item);
        }
        return normalized;
    }

    private void validateLinesForFactory(Long factoryId, List<EcStocktakeOrderLine> lines) {
        if (factoryId == null) {
            return;
        }
        for (EcStocktakeOrderLine line : lines) {
            EcSku sku = ecSkuMapper.selectOne(new LambdaQueryWrapper<EcSku>()
                    .eq(EcSku::getSkuCode, line.getSkuCode()));
            if (sku == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不存在：" + line.getSkuCode());
            }
            EcProduct product = ecProductMapper.selectById(sku.getProductId());
            if (product == null || !factoryId.equals(product.getFactoryId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "货号 " + line.getSkuCode() + " 不属于所选工厂");
            }
        }
    }

    private void saveLines(Long orderId, List<NormalizedLine> lines) {
        for (NormalizedLine item : lines) {
            EcStocktakeOrderLine line = new EcStocktakeOrderLine();
            line.setOrderId(orderId);
            line.setSkuCode(item.skuCode);
            line.setBookQuantity(item.bookQuantity);
            line.setActualQuantity(item.actualQuantity);
            ecStocktakeOrderLineMapper.insert(line);
        }
    }

    private List<EcStocktakeOrderLineVO> loadLines(Long orderId) {
        List<EcStocktakeOrderLine> lines = ecStocktakeOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcStocktakeOrderLine>()
                        .eq(EcStocktakeOrderLine::getOrderId, orderId)
                        .orderByAsc(EcStocktakeOrderLine::getId));
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<String, SkuBrief> skuBriefMap = loadSkuBriefMap(lines.stream()
                .map(EcStocktakeOrderLine::getSkuCode)
                .toList());
        List<EcStocktakeOrderLineVO> result = new ArrayList<>();
        for (EcStocktakeOrderLine line : lines) {
            EcStocktakeOrderLineVO vo = new EcStocktakeOrderLineVO();
            vo.setId(line.getId());
            vo.setSkuCode(line.getSkuCode());
            vo.setBookQuantity(line.getBookQuantity());
            vo.setActualQuantity(line.getActualQuantity());
            SkuBrief brief = skuBriefMap.get(line.getSkuCode());
            if (brief != null) {
                vo.setSpecName(brief.specName);
                vo.setProductName(brief.productName);
            }
            result.add(vo);
        }
        return result;
    }

    private Map<String, SkuBrief> loadSkuBriefMap(List<String> skuCodes) {
        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .in(EcSku::getSkuCode, skuCodes));
        if (skus.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> productNameMap = ecProductMapper.selectBatchIds(
                        skus.stream().map(EcSku::getProductId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(EcProduct::getId, EcProduct::getName, (a, b) -> a));
        Map<String, SkuBrief> result = new HashMap<>();
        for (EcSku sku : skus) {
            SkuBrief brief = new SkuBrief();
            brief.specName = sku.getSpecName();
            brief.productName = productNameMap.get(sku.getProductId());
            result.put(sku.getSkuCode(), brief);
        }
        return result;
    }

    private EcStocktakeOrderDetailVO toDetailVO(EcStocktakeOrder order,
                                                List<EcStocktakeOrderLineVO> lines,
                                                Map<Long, String> factoryNameMap) {
        EcStocktakeOrderDetailVO vo = new EcStocktakeOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setFactoryId(order.getFactoryId());
        if (order.getFactoryId() != null) {
            vo.setFactoryName(factoryNameMap.get(order.getFactoryId()));
        }
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setStocktakeTime(order.getStocktakeTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setLines(lines);
        return vo;
    }

    private Map<Long, String> loadFactoryNameMap(List<Long> factoryIds) {
        if (factoryIds == null || factoryIds.isEmpty()) {
            return Map.of();
        }
        return ecFactoryMapper.selectBatchIds(factoryIds).stream()
                .collect(Collectors.toMap(EcFactory::getId, EcFactory::getName, (a, b) -> a));
    }

    private String generateOrderNo() {
        return "ST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private LocalDateTime requireStocktakeTime(LocalDateTime stocktakeTime) {
        if (stocktakeTime == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "盘点时间不能为空");
        }
        return stocktakeTime;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static final class NormalizedLine {
        private String skuCode;
        private Integer bookQuantity;
        private Integer actualQuantity;
    }

    private static final class SkuBrief {
        private String specName;
        private String productName;
    }
}
