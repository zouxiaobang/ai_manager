package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcInboundOrderConfirmLineItem;
import com.ai.manager.system.domain.dto.EcInboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcInboundOrderLineItem;
import com.ai.manager.system.domain.dto.EcInboundOrderSaveRequest;
import com.ai.manager.system.domain.dto.EcInventoryInboundRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcInboundOrder;
import com.ai.manager.system.domain.entity.EcInboundOrderLine;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.EcInboundOrderDetailVO;
import com.ai.manager.system.domain.vo.EcInboundOrderLineVO;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcInboundOrderLineMapper;
import com.ai.manager.system.mapper.EcInboundOrderMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.service.EcInboundOrderService;
import com.ai.manager.system.service.EcInventoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
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
public class EcInboundOrderServiceImpl extends ServiceImpl<EcInboundOrderMapper, EcInboundOrder>
        implements EcInboundOrderService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String REF_INBOUND_ORDER = "INBOUND_ORDER";

    private final EcInboundOrderLineMapper ecInboundOrderLineMapper;
    private final EcInventoryService ecInventoryService;
    private final EcSkuMapper ecSkuMapper;
    private final EcProductMapper ecProductMapper;
    private final EcFactoryMapper ecFactoryMapper;

    @Override
    public PageResult<EcInboundOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                           String orderMonth, Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcInboundOrder> wrapper = new LambdaQueryWrapper<EcInboundOrder>()
                .orderByDesc(EcInboundOrder::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(EcInboundOrder::getStatus, status.trim().toUpperCase());
        }
        if (factoryId != null) {
            wrapper.eq(EcInboundOrder::getFactoryId, factoryId);
        }
        if (StringUtils.hasText(orderMonth)) {
            applyOrderMonthFilter(wrapper, orderMonth.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcInboundOrder::getOrderNo, kw).or().like(EcInboundOrder::getRemark, kw));
        }
        Page<EcInboundOrder> entityPage = page(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            return PageUtils.of(List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(entityPage.getRecords().stream()
                .map(EcInboundOrder::getFactoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        List<EcInboundOrderDetailVO> records = new ArrayList<>();
        for (EcInboundOrder order : entityPage.getRecords()) {
            records.add(toDetailVO(order, loadLines(order.getId()), factoryNameMap));
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public EcInboundOrderDetailVO getOrderDetail(Long id) {
        EcInboundOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(
                order.getFactoryId() == null ? List.of() : List.of(order.getFactoryId()));
        return toDetailVO(order, loadLines(id), factoryNameMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInboundOrderDetailVO createOrder(EcInboundOrderSaveRequest request) {
        List<EcInboundOrderLineItem> lines = validateAndNormalizeLines(request, null);
        EcInboundOrder order = new EcInboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setFactoryId(request.getFactoryId());
        order.setStatus(STATUS_DRAFT);
        order.setRemark(trimToNull(request.getRemark()));
        order.setOrderTime(requireOrderTime(request.getOrderTime()));
        order.setExpectedDeliveryTime(requireExpectedDeliveryTime(request.getExpectedDeliveryTime()));
        save(order);
        saveLines(order.getId(), lines);
        return getOrderDetail(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInboundOrderDetailVO updateOrder(Long id, EcInboundOrderSaveRequest request) {
        EcInboundOrder order = requireDraftOrder(id);
        List<EcInboundOrderLineItem> lines = validateAndNormalizeLines(request, order.getFactoryId());
        order.setFactoryId(request.getFactoryId());
        order.setRemark(trimToNull(request.getRemark()));
        order.setOrderTime(requireOrderTime(request.getOrderTime()));
        order.setExpectedDeliveryTime(requireExpectedDeliveryTime(request.getExpectedDeliveryTime()));
        updateById(order);
        ecInboundOrderLineMapper.delete(new LambdaQueryWrapper<EcInboundOrderLine>()
                .eq(EcInboundOrderLine::getOrderId, id));
        saveLines(id, lines);
        return getOrderDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInboundOrderDetailVO confirmOrder(Long id, EcInboundOrderConfirmRequest request) {
        EcInboundOrder order = requireDraftOrder(id);
        List<EcInboundOrderLine> lines = ecInboundOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcInboundOrderLine>()
                        .eq(EcInboundOrderLine::getOrderId, id)
                        .orderByAsc(EcInboundOrderLine::getId));
        if (lines.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "进货单至少包含一条明细");
        }
        validateLinesForFactory(order.getFactoryId(), lines);

        Map<Long, Integer> receivedQtyMap = parseConfirmLines(lines, request);
        int inboundTotal = 0;
        String remark = "进货单 " + order.getOrderNo();
        for (EcInboundOrderLine line : lines) {
            ecInventoryService.requireSkuAvailableForInbound(line.getSkuCode());
            int receivedQty = receivedQtyMap.get(line.getId());
            line.setReceivedQuantity(receivedQty);
            ecInboundOrderLineMapper.updateById(line);
            if (receivedQty <= 0) {
                continue;
            }
            inboundTotal += receivedQty;
            EcInventoryInboundRequest inboundRequest = new EcInventoryInboundRequest();
            inboundRequest.setSkuCode(line.getSkuCode());
            inboundRequest.setQuantity(receivedQty);
            if (!Objects.equals(receivedQty, line.getQuantity())) {
                inboundRequest.setRemark(remark + "（下单" + line.getQuantity() + "，实收" + receivedQty + "）");
            } else {
                inboundRequest.setRemark(remark);
            }
            ecInventoryService.inbound(inboundRequest, REF_INBOUND_ORDER, order.getId());
        }
        if (inboundTotal <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "至少一条明细实收数量大于 0");
        }

        order.setStatus(STATUS_CONFIRMED);
        order.setActualReceiptTime(LocalDateTime.now());
        updateById(order);
        return getOrderDetail(id);
    }

    private Map<Long, Integer> parseConfirmLines(List<EcInboundOrderLine> lines,
                                                 EcInboundOrderConfirmRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请填写实收数量");
        }
        Map<Long, EcInboundOrderLine> lineMap = lines.stream()
                .collect(Collectors.toMap(EcInboundOrderLine::getId, line -> line, (a, b) -> a));
        Map<Long, Integer> result = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        for (EcInboundOrderConfirmLineItem item : request.getLines()) {
            if (item == null || item.getLineId() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细 ID 不能为空");
            }
            if (!seen.add(item.getLineId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "存在重复明细");
            }
            EcInboundOrderLine line = lineMap.get(item.getLineId());
            if (line == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细不属于当前进货单");
            }
            if (item.getReceivedQuantity() == null || item.getReceivedQuantity() < 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "货号 " + line.getSkuCode() + " 实收数量不能为负");
            }
            result.put(item.getLineId(), item.getReceivedQuantity());
        }
        for (EcInboundOrderLine line : lines) {
            if (!result.containsKey(line.getId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "缺少货号 " + line.getSkuCode() + " 的实收数量");
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        EcInboundOrder order = requireDraftOrder(id);
        order.setStatus(STATUS_CANCELLED);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        EcInboundOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅草稿进货单可删除");
        }
        ecInboundOrderLineMapper.delete(new LambdaQueryWrapper<EcInboundOrderLine>()
                .eq(EcInboundOrderLine::getOrderId, id));
        removeById(id);
    }

    private EcInboundOrder requireDraftOrder(Long id) {
        EcInboundOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅草稿进货单可编辑或确认");
        }
        return order;
    }

    private List<EcInboundOrderLineItem> validateAndNormalizeLines(EcInboundOrderSaveRequest request,
                                                                   Long existingFactoryId) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "进货单至少包含一条明细");
        }
        Long factoryId = request.getFactoryId() != null ? request.getFactoryId() : existingFactoryId;
        if (factoryId != null) {
            EcFactory factory = ecFactoryMapper.selectById(factoryId);
            if (factory == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工厂不存在");
            }
            requireProductionFactoryForSkuOrder(factory);
        }

        List<EcInboundOrderLineItem> normalized = new ArrayList<>();
        Set<String> skuCodes = new HashSet<>();
        for (EcInboundOrderLineItem line : request.getLines()) {
            if (line == null || !StringUtils.hasText(line.getSkuCode())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细货号不能为空");
            }
            String skuCode = line.getSkuCode().trim();
            if (!skuCodes.add(skuCode)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "进货单存在重复货号：" + skuCode);
            }
            if (line.getQuantity() == null || line.getQuantity() <= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细数量必须大于 0");
            }
            EcSku sku = ecSkuMapper.selectOne(new LambdaQueryWrapper<EcSku>().eq(EcSku::getSkuCode, skuCode));
            if (sku == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "SKU 货号不存在：" + skuCode);
            }
            ecInventoryService.requireSkuAvailableForInbound(skuCode);
            if (factoryId != null) {
                EcProduct product = ecProductMapper.selectById(sku.getProductId());
                if (product == null || !factoryId.equals(product.getFactoryId())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                            "货号 " + skuCode + " 不属于所选工厂");
                }
            }
            EcInboundOrderLineItem item = new EcInboundOrderLineItem();
            item.setSkuCode(skuCode);
            item.setQuantity(line.getQuantity());
            normalized.add(item);
        }
        return normalized;
    }

    private void validateLinesForFactory(Long factoryId, List<EcInboundOrderLine> lines) {
        if (factoryId == null) {
            return;
        }
        for (EcInboundOrderLine line : lines) {
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

    private void saveLines(Long orderId, List<EcInboundOrderLineItem> lines) {
        for (EcInboundOrderLineItem item : lines) {
            EcInboundOrderLine line = new EcInboundOrderLine();
            line.setOrderId(orderId);
            line.setSkuCode(item.getSkuCode());
            line.setQuantity(item.getQuantity());
            ecInboundOrderLineMapper.insert(line);
        }
    }

    private List<EcInboundOrderLineVO> loadLines(Long orderId) {
        List<EcInboundOrderLine> lines = ecInboundOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcInboundOrderLine>()
                        .eq(EcInboundOrderLine::getOrderId, orderId)
                        .orderByAsc(EcInboundOrderLine::getId));
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<String, SkuBrief> skuBriefMap = loadSkuBriefMap(lines.stream()
                .map(EcInboundOrderLine::getSkuCode)
                .toList());
        List<EcInboundOrderLineVO> result = new ArrayList<>();
        for (EcInboundOrderLine line : lines) {
            EcInboundOrderLineVO vo = new EcInboundOrderLineVO();
            vo.setId(line.getId());
            vo.setSkuCode(line.getSkuCode());
            vo.setQuantity(line.getQuantity());
            vo.setReceivedQuantity(line.getReceivedQuantity());
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

    private EcInboundOrderDetailVO toDetailVO(EcInboundOrder order,
                                              List<EcInboundOrderLineVO> lines,
                                              Map<Long, String> factoryNameMap) {
        EcInboundOrderDetailVO vo = new EcInboundOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setFactoryId(order.getFactoryId());
        if (order.getFactoryId() != null) {
            vo.setFactoryName(factoryNameMap.get(order.getFactoryId()));
        }
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setOrderTime(order.getOrderTime());
        vo.setExpectedDeliveryTime(order.getExpectedDeliveryTime());
        vo.setActualReceiptTime(order.getActualReceiptTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setLines(lines);
        return vo;
    }

    private void requireProductionFactoryForSkuOrder(EcFactory factory) {
        String type = factory.getFactoryType();
        if (type == null || type.isBlank()) {
            return;
        }
        if (!"PRODUCTION".equals(type.trim().toUpperCase())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "进货单请选择生产工厂，纸箱进出货请在纸箱管理中处理");
        }
    }

    private Map<Long, String> loadFactoryNameMap(List<Long> factoryIds) {
        if (factoryIds == null || factoryIds.isEmpty()) {
            return Map.of();
        }
        return ecFactoryMapper.selectBatchIds(factoryIds).stream()
                .collect(Collectors.toMap(EcFactory::getId, EcFactory::getName, (a, b) -> a));
    }

    private void applyOrderMonthFilter(LambdaQueryWrapper<EcInboundOrder> wrapper, String orderMonth) {
        try {
            YearMonth ym = YearMonth.parse(orderMonth);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);
            wrapper.between(EcInboundOrder::getOrderTime, start, end);
        } catch (Exception ignored) {
            /* ignore invalid month */
        }
    }

    private String generateOrderNo() {
        return "IN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private LocalDateTime requireOrderTime(LocalDateTime orderTime) {
        if (orderTime == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "下单时间不能为空");
        }
        return orderTime;
    }

    private LocalDateTime requireExpectedDeliveryTime(LocalDateTime expectedDeliveryTime) {
        if (expectedDeliveryTime == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预收货时间不能为空");
        }
        return expectedDeliveryTime;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static final class SkuBrief {
        private String specName;
        private String productName;
    }
}
