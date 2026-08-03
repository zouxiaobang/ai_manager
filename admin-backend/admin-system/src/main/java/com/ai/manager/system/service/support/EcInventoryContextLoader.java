package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcInboundOrder;
import com.ai.manager.system.domain.entity.EcInboundOrderLine;
import com.ai.manager.system.domain.entity.EcOutboundOrder;
import com.ai.manager.system.domain.entity.EcOutboundOrderLine;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.EcInventoryInboundBriefVO;
import com.ai.manager.system.domain.vo.EcInventoryOutboundBriefVO;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcInboundOrderLineMapper;
import com.ai.manager.system.mapper.EcInboundOrderMapper;
import com.ai.manager.system.mapper.EcOutboundOrderLineMapper;
import com.ai.manager.system.mapper.EcOutboundOrderMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.service.support.EcInventoryVoAssembler.SkuBrief;
import com.ai.manager.system.service.support.EcInventoryVoAssembler.SkuContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 电商库存视图上下文加载器
 *
 * <p>从 {@code EcInventoryServiceImpl} 中提取的只读数据访问逻辑：SKU 上下文/简讯、
 * 在途量、相关出入库订单等列表/详情展示所需的派生数据。与 {@link EcInventoryVoAssembler}
 * 配合（加载器产出上下文，组装器映射 VO），让库存服务主类只保留业务编排与写操作。</p>
 */
@Component
@RequiredArgsConstructor
public class EcInventoryContextLoader {

    private static final String STATUS_DRAFT = "DRAFT";

    private final EcSkuMapper ecSkuMapper;
    private final EcProductMapper ecProductMapper;
    private final EcFactoryMapper ecFactoryMapper;
    private final EcCartonMapper ecCartonMapper;
    private final EcInboundOrderMapper ecInboundOrderMapper;
    private final EcInboundOrderLineMapper ecInboundOrderLineMapper;
    private final EcOutboundOrderMapper ecOutboundOrderMapper;
    private final EcOutboundOrderLineMapper ecOutboundOrderLineMapper;

    /** SKU 简讯：货号 → 商品维度摘要（列表行展示） */
    public Map<String, SkuBrief> loadSkuBriefMap(List<String> skuCodes) {
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
            brief.productId = sku.getProductId();
            brief.imageName = sku.getImageName();
            brief.salePrice = sku.getSalePrice();
            result.put(sku.getSkuCode().trim(), brief);
        }
        return result;
    }

    /** 单个货号在途量（草稿入库单中未到货数量） */
    public int loadInTransitQty(String skuCode) {
        return loadInTransitMap(List.of(skuCode)).getOrDefault(skuCode, 0);
    }

    /** 货号 → 在途量：聚合草稿入库单各行的计划数量 */
    public Map<String, Integer> loadInTransitMap(List<String> skuCodes) {
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

    /** 相关入库单简报：按行去重取单、按单号倒序（详情关联展示） */
    public List<EcInventoryInboundBriefVO> loadRelatedInboundOrders(String skuCode) {
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

    /** 相关出库单简报：按行去重取单、按单号倒序（详情关联展示） */
    public List<EcInventoryOutboundBriefVO> loadRelatedOutboundOrders(String skuCode) {
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

    /** 单个货号 SKU 上下文（详情/装箱估算） */
    public SkuContext loadSkuContext(String skuCode) {
        Map<String, SkuContext> map = loadSkuContextMap(List.of(skuCode));
        return map.get(skuCode);
    }

    /** 货号 → SKU 上下文：SKU + 商品 + 工厂 + 纸箱维度一次性加载组装 */
    public Map<String, SkuContext> loadSkuContextMap(List<String> skuCodes) {
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
}
