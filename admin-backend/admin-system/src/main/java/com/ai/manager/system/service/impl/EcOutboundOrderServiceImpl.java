package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcInventoryOutboundRequest;
import com.ai.manager.system.domain.dto.EcOutboundOrderConfirmLineItem;
import com.ai.manager.system.domain.dto.EcOutboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcOutboundOrderLineItem;
import com.ai.manager.system.domain.dto.EcOutboundOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcOutboundOrder;
import com.ai.manager.system.domain.entity.EcOutboundOrderLine;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.EcOutboundOrderDetailVO;
import com.ai.manager.system.domain.vo.EcOutboundOrderLineVO;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcOutboundOrderLineMapper;
import com.ai.manager.system.mapper.EcOutboundOrderMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.service.EcInventoryService;
import com.ai.manager.system.service.EcOutboundOrderService;
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
public class EcOutboundOrderServiceImpl extends ServiceImpl<EcOutboundOrderMapper, EcOutboundOrder>
        implements EcOutboundOrderService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String REF_OUTBOUND_ORDER = "OUTBOUND_ORDER";
    private static final String FACTORY_TYPE_PRODUCTION = "PRODUCTION";
    private static final String FACTORY_TYPE_CUSTOMER = "CUSTOMER";

    private final EcOutboundOrderLineMapper ecOutboundOrderLineMapper;
    private final EcInventoryService ecInventoryService;
    private final EcSkuMapper ecSkuMapper;
    private final EcProductMapper ecProductMapper;
    private final EcFactoryMapper ecFactoryMapper;

    @Override
    public PageResult<EcOutboundOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                          Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcOutboundOrder> wrapper = new LambdaQueryWrapper<EcOutboundOrder>()
                .orderByDesc(EcOutboundOrder::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(EcOutboundOrder::getStatus, status.trim().toUpperCase());
        }
        if (factoryId != null) {
            wrapper.eq(EcOutboundOrder::getFactoryId, factoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcOutboundOrder::getOrderNo, kw).or().like(EcOutboundOrder::getRemark, kw));
        }
        Page<EcOutboundOrder> entityPage = page(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            return PageUtils.of(List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(entityPage.getRecords().stream()
                .map(EcOutboundOrder::getFactoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Map<Long, EcFactory> customerFactoryMap = loadCustomerFactoryMap(entityPage.getRecords().stream()
                .map(EcOutboundOrder::getCustomerFactoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        List<EcOutboundOrderDetailVO> records = new ArrayList<>();
        for (EcOutboundOrder order : entityPage.getRecords()) {
            records.add(toDetailVO(order, loadLines(order.getId()), factoryNameMap, customerFactoryMap));
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public EcOutboundOrderDetailVO getOrderDetail(Long id) {
        EcOutboundOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(
                order.getFactoryId() == null ? List.of() : List.of(order.getFactoryId()));
        Map<Long, EcFactory> customerFactoryMap = loadCustomerFactoryMap(
                order.getCustomerFactoryId() == null ? List.of() : List.of(order.getCustomerFactoryId()));
        return toDetailVO(order, loadLines(id), factoryNameMap, customerFactoryMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcOutboundOrderDetailVO createOrder(EcOutboundOrderSaveRequest request) {
        List<EcOutboundOrderLineItem> lines = validateAndNormalizeLines(request, null);
        EcOutboundOrder order = new EcOutboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setFactoryId(request.getFactoryId());
        order.setCustomerFactoryId(request.getCustomerFactoryId());
        order.setStatus(STATUS_DRAFT);
        order.setRemark(trimToNull(request.getRemark()));
        order.setOrderTime(requireOrderTime(request.getOrderTime()));
        order.setExpectedShipTime(requireExpectedShipTime(request.getExpectedShipTime()));
        save(order);
        saveLines(order.getId(), lines);
        return getOrderDetail(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcOutboundOrderDetailVO updateOrder(Long id, EcOutboundOrderSaveRequest request) {
        EcOutboundOrder order = requireDraftOrder(id);
        List<EcOutboundOrderLineItem> lines = validateAndNormalizeLines(request, order.getFactoryId());
        order.setFactoryId(request.getFactoryId());
        order.setCustomerFactoryId(request.getCustomerFactoryId());
        order.setRemark(trimToNull(request.getRemark()));
        order.setOrderTime(requireOrderTime(request.getOrderTime()));
        order.setExpectedShipTime(requireExpectedShipTime(request.getExpectedShipTime()));
        updateById(order);
        ecOutboundOrderLineMapper.delete(new LambdaQueryWrapper<EcOutboundOrderLine>()
                .eq(EcOutboundOrderLine::getOrderId, id));
        saveLines(id, lines);
        return getOrderDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcOutboundOrderDetailVO confirmOrder(Long id, EcOutboundOrderConfirmRequest request) {
        EcOutboundOrder order = requireDraftOrder(id);
        List<EcOutboundOrderLine> lines = ecOutboundOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcOutboundOrderLine>()
                        .eq(EcOutboundOrderLine::getOrderId, id)
                        .orderByAsc(EcOutboundOrderLine::getId));
        if (lines.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "出货单至少包含一条明细");
        }
        validateLinesForFactory(order.getFactoryId(), lines);

        Map<Long, Integer> shippedQtyMap = parseConfirmLines(lines, request);
        int outboundTotal = 0;
        String remark = "出货单 " + order.getOrderNo();
        for (EcOutboundOrderLine line : lines) {
            int shippedQty = shippedQtyMap.get(line.getId());
            line.setShippedQuantity(shippedQty);
            ecOutboundOrderLineMapper.updateById(line);
            if (shippedQty <= 0) {
                continue;
            }
            outboundTotal += shippedQty;
            EcInventoryOutboundRequest outboundRequest = new EcInventoryOutboundRequest();
            outboundRequest.setSkuCode(line.getSkuCode());
            outboundRequest.setQuantity(shippedQty);
            if (!Objects.equals(shippedQty, line.getQuantity())) {
                outboundRequest.setRemark(remark + "（计划" + line.getQuantity() + "，实出" + shippedQty + "）");
            } else {
                outboundRequest.setRemark(remark);
            }
            ecInventoryService.outbound(outboundRequest, REF_OUTBOUND_ORDER, order.getId());
        }
        if (outboundTotal <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "至少一条明细实出数量大于 0");
        }

        order.setStatus(STATUS_CONFIRMED);
        order.setActualShipTime(LocalDateTime.now());
        updateById(order);
        return getOrderDetail(id);
    }

    private Map<Long, Integer> parseConfirmLines(List<EcOutboundOrderLine> lines,
                                                 EcOutboundOrderConfirmRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请填写实出数量");
        }
        Map<Long, EcOutboundOrderLine> lineMap = lines.stream()
                .collect(Collectors.toMap(EcOutboundOrderLine::getId, line -> line, (a, b) -> a));
        Map<Long, Integer> result = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        for (EcOutboundOrderConfirmLineItem item : request.getLines()) {
            if (item == null || item.getLineId() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细 ID 不能为空");
            }
            if (!seen.add(item.getLineId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "存在重复明细");
            }
            EcOutboundOrderLine line = lineMap.get(item.getLineId());
            if (line == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细不属于当前出货单");
            }
            if (item.getShippedQuantity() == null || item.getShippedQuantity() < 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "货号 " + line.getSkuCode() + " 实出数量不能为负");
            }
            result.put(item.getLineId(), item.getShippedQuantity());
        }
        for (EcOutboundOrderLine line : lines) {
            if (!result.containsKey(line.getId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "缺少货号 " + line.getSkuCode() + " 的实出数量");
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        EcOutboundOrder order = requireDraftOrder(id);
        order.setStatus(STATUS_CANCELLED);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        EcOutboundOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅草稿出货单可删除");
        }
        ecOutboundOrderLineMapper.delete(new LambdaQueryWrapper<EcOutboundOrderLine>()
                .eq(EcOutboundOrderLine::getOrderId, id));
        removeById(id);
    }

    private EcOutboundOrder requireDraftOrder(Long id) {
        EcOutboundOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅草稿出货单可编辑或确认");
        }
        return order;
    }

    private List<EcOutboundOrderLineItem> validateAndNormalizeLines(EcOutboundOrderSaveRequest request,
                                                                    Long existingFactoryId) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "出货单至少包含一条明细");
        }
        Long factoryId = request.getFactoryId() != null ? request.getFactoryId() : existingFactoryId;
        if (factoryId != null) {
            EcFactory factory = requireFactory(factoryId);
            if (!FACTORY_TYPE_PRODUCTION.equals(effectiveFactoryType(factory))) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择生产类型工厂，纸箱进出货请在纸箱管理中处理");
            }
        }
        if (request.getCustomerFactoryId() != null) {
            EcFactory customer = requireFactory(request.getCustomerFactoryId());
            if (!FACTORY_TYPE_CUSTOMER.equals(effectiveFactoryType(customer))) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择客户类型工厂");
            }
        }

        List<EcOutboundOrderLineItem> normalized = new ArrayList<>();
        Set<String> skuCodes = new HashSet<>();
        for (EcOutboundOrderLineItem line : request.getLines()) {
            if (line == null || !StringUtils.hasText(line.getSkuCode())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细货号不能为空");
            }
            String skuCode = line.getSkuCode().trim();
            if (!skuCodes.add(skuCode)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "出货单存在重复货号：" + skuCode);
            }
            if (line.getQuantity() == null || line.getQuantity() <= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细数量必须大于 0");
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
            EcOutboundOrderLineItem item = new EcOutboundOrderLineItem();
            item.setSkuCode(skuCode);
            item.setQuantity(line.getQuantity());
            normalized.add(item);
        }
        return normalized;
    }

    private void validateLinesForFactory(Long factoryId, List<EcOutboundOrderLine> lines) {
        if (factoryId == null) {
            return;
        }
        for (EcOutboundOrderLine line : lines) {
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

    private void saveLines(Long orderId, List<EcOutboundOrderLineItem> lines) {
        for (EcOutboundOrderLineItem item : lines) {
            EcOutboundOrderLine line = new EcOutboundOrderLine();
            line.setOrderId(orderId);
            line.setSkuCode(item.getSkuCode());
            line.setQuantity(item.getQuantity());
            ecOutboundOrderLineMapper.insert(line);
        }
    }

    private List<EcOutboundOrderLineVO> loadLines(Long orderId) {
        List<EcOutboundOrderLine> lines = ecOutboundOrderLineMapper.selectList(
                new LambdaQueryWrapper<EcOutboundOrderLine>()
                        .eq(EcOutboundOrderLine::getOrderId, orderId)
                        .orderByAsc(EcOutboundOrderLine::getId));
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<String, SkuBrief> skuBriefMap = loadSkuBriefMap(lines.stream()
                .map(EcOutboundOrderLine::getSkuCode)
                .toList());
        List<EcOutboundOrderLineVO> result = new ArrayList<>();
        for (EcOutboundOrderLine line : lines) {
            EcOutboundOrderLineVO vo = new EcOutboundOrderLineVO();
            vo.setId(line.getId());
            vo.setSkuCode(line.getSkuCode());
            vo.setQuantity(line.getQuantity());
            vo.setShippedQuantity(line.getShippedQuantity());
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

    private EcOutboundOrderDetailVO toDetailVO(EcOutboundOrder order,
                                             List<EcOutboundOrderLineVO> lines,
                                             Map<Long, String> factoryNameMap,
                                             Map<Long, EcFactory> customerFactoryMap) {
        EcOutboundOrderDetailVO vo = new EcOutboundOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setFactoryId(order.getFactoryId());
        if (order.getFactoryId() != null) {
            vo.setFactoryName(factoryNameMap.get(order.getFactoryId()));
        }
        vo.setCustomerFactoryId(order.getCustomerFactoryId());
        if (order.getCustomerFactoryId() != null) {
            EcFactory customer = customerFactoryMap.get(order.getCustomerFactoryId());
            if (customer != null) {
                vo.setCustomerName(customer.getName());
                vo.setCustomerContactName(customer.getContactName());
                vo.setCustomerContactPhone(customer.getContactPhone());
                vo.setCustomerAddress(customer.getAddress());
            }
        }
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setOrderTime(order.getOrderTime());
        vo.setExpectedShipTime(order.getExpectedShipTime());
        vo.setActualShipTime(order.getActualShipTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setLines(lines);
        return vo;
    }

    private EcFactory requireFactory(Long factoryId) {
        EcFactory factory = ecFactoryMapper.selectById(factoryId);
        if (factory == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工厂不存在");
        }
        return factory;
    }

    private String effectiveFactoryType(EcFactory factory) {
        if (factory == null || !StringUtils.hasText(factory.getFactoryType())) {
            return FACTORY_TYPE_PRODUCTION;
        }
        return factory.getFactoryType().trim().toUpperCase();
    }

    private Map<Long, EcFactory> loadCustomerFactoryMap(List<Long> factoryIds) {
        if (factoryIds == null || factoryIds.isEmpty()) {
            return Map.of();
        }
        return ecFactoryMapper.selectBatchIds(factoryIds).stream()
                .collect(Collectors.toMap(EcFactory::getId, f -> f, (a, b) -> a));
    }

    private Map<Long, String> loadFactoryNameMap(List<Long> factoryIds) {
        if (factoryIds == null || factoryIds.isEmpty()) {
            return Map.of();
        }
        return ecFactoryMapper.selectBatchIds(factoryIds).stream()
                .collect(Collectors.toMap(EcFactory::getId, EcFactory::getName, (a, b) -> a));
    }

    private String generateOrderNo() {
        return "OUT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private LocalDateTime requireOrderTime(LocalDateTime orderTime) {
        if (orderTime == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "创单时间不能为空");
        }
        return orderTime;
    }

    private LocalDateTime requireExpectedShipTime(LocalDateTime expectedShipTime) {
        if (expectedShipTime == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "预出货时间不能为空");
        }
        return expectedShipTime;
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
