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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * EcInventoryContextLoader 单元测试
 * mock 各 mapper 断言 SKU 上下文/简讯、在途量、相关出入库订单的加载组装。
 */
@ExtendWith(MockitoExtension.class)
class EcInventoryContextLoaderTest {

    @Mock private EcSkuMapper ecSkuMapper;
    @Mock private EcProductMapper ecProductMapper;
    @Mock private EcFactoryMapper ecFactoryMapper;
    @Mock private EcCartonMapper ecCartonMapper;
    @Mock private EcInboundOrderMapper ecInboundOrderMapper;
    @Mock private EcInboundOrderLineMapper ecInboundOrderLineMapper;
    @Mock private EcOutboundOrderMapper ecOutboundOrderMapper;
    @Mock private EcOutboundOrderLineMapper ecOutboundOrderLineMapper;

    @InjectMocks private EcInventoryContextLoader loader;

    private EcSku sku(Long id, String skuCode, Long productId) {
        EcSku s = new EcSku();
        s.setId(id);
        s.setSkuCode(skuCode);
        s.setProductId(productId);
        s.setSpecName("红色/L");
        s.setSalePrice(new BigDecimal("19.90"));
        s.setStatus("ON_SALE");
        s.setUnitsPerCarton(10);
        s.setCartonId(2L);
        return s;
    }

    private EcProduct product(Long id, Long factoryId) {
        EcProduct p = new EcProduct();
        p.setId(id);
        p.setName("商品甲");
        p.setFactoryId(factoryId);
        p.setStatus("ENABLED");
        return p;
    }

    // ==================== SKU 简讯 ====================

    @Test
    void loadSkuBriefMap_shouldBuildBriefPerSkuWithProductName() {
        when(ecSkuMapper.selectList(any())).thenReturn(List.of(sku(3L, "SKU-001", 7L)));
        when(ecProductMapper.selectBatchIds(anyCollection())).thenReturn(List.of(product(7L, null)));

        Map<String, SkuBrief> map = loader.loadSkuBriefMap(List.of("SKU-001"));

        SkuBrief brief = map.get("SKU-001");
        assertThat(brief).isNotNull();
        assertThat(brief.specName).isEqualTo("红色/L");
        assertThat(brief.productName).isEqualTo("商品甲");
        assertThat(brief.productId).isEqualTo(7L);
        assertThat(brief.salePrice).isEqualByComparingTo(new BigDecimal("19.90"));
    }

    @Test
    void loadSkuBriefMap_shouldTrimSkuCodeKey() {
        when(ecSkuMapper.selectList(any())).thenReturn(List.of(sku(3L, "SKU-001 ", 7L)));
        when(ecProductMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        Map<String, SkuBrief> map = loader.loadSkuBriefMap(List.of("SKU-001"));

        assertThat(map.get("SKU-001")).isNotNull();
        assertThat(map.get("SKU-001 ")).isNull();
    }

    @Test
    void loadSkuBriefMap_shouldReturnEmptyForNullOrEmptyInput() {
        assertThat(loader.loadSkuBriefMap(null)).isEmpty();
        assertThat(loader.loadSkuBriefMap(List.of())).isEmpty();
    }

    @Test
    void loadSkuBriefMap_shouldReturnEmptyWhenNoSkusMatched() {
        when(ecSkuMapper.selectList(any())).thenReturn(List.of());
        assertThat(loader.loadSkuBriefMap(List.of("SKU-X"))).isEmpty();
    }

    // ==================== 在途量 ====================

    @Test
    void loadInTransitMap_shouldSumDraftLineQuantities() {
        when(ecInboundOrderMapper.selectList(any())).thenReturn(List.of(order(100L, "DRAFT"), order(101L, "DRAFT")));
        when(ecInboundOrderLineMapper.selectList(any())).thenReturn(List.of(
                inboundLine(100L, "SKU-001", 2, 1),
                inboundLine(101L, "SKU-001", 3, 2),
                inboundLine(101L, "SKU-002", 5, 5)));

        Map<String, Integer> map = loader.loadInTransitMap(List.of("SKU-001", "SKU-002"));

        // SKU-001 两单合计 5，SKU-002 仅 5（全部到货也算在途计划量）
        assertThat(map).containsEntry("SKU-001", 5).containsEntry("SKU-002", 5);
    }

    @Test
    void loadInTransitMap_shouldReturnZeroWhenNoDraftOrders() {
        when(ecInboundOrderMapper.selectList(any())).thenReturn(List.of());
        Map<String, Integer> map = loader.loadInTransitMap(List.of("SKU-001"));
        assertThat(map).containsEntry("SKU-001", 0);
    }

    @Test
    void loadInTransitMap_shouldReturnEmptyForNullOrEmptyInput() {
        assertThat(loader.loadInTransitMap(null)).isEmpty();
        assertThat(loader.loadInTransitMap(List.of())).isEmpty();
    }

    @Test
    void loadInTransitQty_shouldReturnSingleSkuInTransit() {
        when(ecInboundOrderMapper.selectList(any())).thenReturn(List.of(order(100L, "DRAFT")));
        when(ecInboundOrderLineMapper.selectList(any())).thenReturn(List.of(inboundLine(100L, "SKU-001", 4, 0)));
        assertThat(loader.loadInTransitQty("SKU-001")).isEqualTo(4);
    }

    // ==================== 相关出入库单 ====================

    @Test
    void loadRelatedInboundOrders_shouldDeduplicateOrdersAndSortDesc() {
        when(ecInboundOrderLineMapper.selectList(any())).thenReturn(List.of(
                inboundLine(100L, "SKU-001", 2, 1),
                inboundLine(100L, "SKU-001", 2, 1),
                inboundLine(101L, "SKU-001", 3, 2)));
        when(ecInboundOrderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                order(100L, "RECEIVED", "IN-100"),
                order(101L, "DRAFT", "IN-101")));

        List<EcInventoryInboundBriefVO> list = loader.loadRelatedInboundOrders("SKU-001");

        // 同一单去重，按 id 倒序
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(101L);
        assertThat(list.get(0).getOrderNo()).isEqualTo("IN-101");
        assertThat(list.get(1).getId()).isEqualTo(100L);
    }

    @Test
    void loadRelatedInboundOrders_shouldReturnEmptyWhenNoLines() {
        when(ecInboundOrderLineMapper.selectList(any())).thenReturn(List.of());
        assertThat(loader.loadRelatedInboundOrders("SKU-001")).isEmpty();
    }

    @Test
    void loadRelatedOutboundOrders_shouldBuildBriefs() {
        when(ecOutboundOrderLineMapper.selectList(any())).thenReturn(List.of(
                outboundLine(200L, "SKU-001", 4, 2),
                outboundLine(201L, "SKU-001", 6, 6)));
        when(ecOutboundOrderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                outboundOrder(200L, "SHIPPED", "OUT-200"),
                outboundOrder(201L, "DRAFT", "OUT-201")));

        List<EcInventoryOutboundBriefVO> list = loader.loadRelatedOutboundOrders("SKU-001");

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(201L);
        assertThat(list.get(0).getOrderNo()).isEqualTo("OUT-201");
        assertThat(list.get(0).getShippedQuantity()).isEqualTo(6);
    }

    // ==================== SKU 上下文 ====================

    @Test
    void loadSkuContextMap_shouldAssembleSkuProductFactoryCarton() {
        when(ecSkuMapper.selectList(any())).thenReturn(List.of(sku(3L, "SKU-001", 7L)));
        when(ecProductMapper.selectBatchIds(anyCollection())).thenReturn(List.of(product(7L, 5L)));
        when(ecFactoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(factory(5L, "工厂甲")));
        when(ecCartonMapper.selectBatchIds(anyCollection())).thenReturn(List.of(carton(2L, "标准箱")));

        Map<String, SkuContext> map = loader.loadSkuContextMap(List.of("SKU-001"));

        SkuContext ctx = map.get("SKU-001");
        assertThat(ctx).isNotNull();
        assertThat(ctx.skuId).isEqualTo(3L);
        assertThat(ctx.productId).isEqualTo(7L);
        assertThat(ctx.productName).isEqualTo("商品甲");
        assertThat(ctx.factoryId).isEqualTo(5L);
        assertThat(ctx.factoryName).isEqualTo("工厂甲");
        assertThat(ctx.cartonId).isEqualTo(2L);
        assertThat(ctx.cartonName).isEqualTo("标准箱");
        assertThat(ctx.cartonLengthCm).isEqualByComparingTo(new BigDecimal("30"));
        assertThat(ctx.unitsPerCarton).isEqualTo(10);
        assertThat(ctx.skuStatus).isEqualTo("ON_SALE");
    }

    @Test
    void loadSkuContextMap_shouldReturnEmptyForNullOrEmptyInput() {
        assertThat(loader.loadSkuContextMap(null)).isEmpty();
        assertThat(loader.loadSkuContextMap(List.of())).isEmpty();
    }

    @Test
    void loadSkuContextMap_shouldReturnEmptyWhenNoSkusMatched() {
        when(ecSkuMapper.selectList(any())).thenReturn(List.of());
        assertThat(loader.loadSkuContextMap(List.of("SKU-X"))).isEmpty();
    }

    @Test
    void loadSkuContext_shouldReturnSingleContext() {
        when(ecSkuMapper.selectList(any())).thenReturn(List.of(sku(3L, "SKU-001", 7L)));
        when(ecProductMapper.selectBatchIds(anyCollection())).thenReturn(List.of(product(7L, 5L)));
        when(ecFactoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(factory(5L, "工厂甲")));
        when(ecCartonMapper.selectBatchIds(anyCollection())).thenReturn(List.of(carton(2L, "标准箱")));

        SkuContext ctx = loader.loadSkuContext("SKU-001");

        assertThat(ctx).isNotNull();
        assertThat(ctx.skuId).isEqualTo(3L);
    }

    @Test
    void loadSkuContext_shouldReturnNullForUnknownSku() {
        when(ecSkuMapper.selectList(any())).thenReturn(List.of());
        assertThat(loader.loadSkuContext("SKU-X")).isNull();
    }

    private EcInboundOrder order(Long id, String status) {
        EcInboundOrder o = new EcInboundOrder();
        o.setId(id);
        o.setStatus(status);
        o.setOrderNo("IN-" + id);
        return o;
    }

    private EcInboundOrder order(Long id, String status, String orderNo) {
        EcInboundOrder o = order(id, status);
        o.setOrderNo(orderNo);
        return o;
    }

    private EcInboundOrderLine inboundLine(Long orderId, String skuCode, int qty, int received) {
        EcInboundOrderLine line = new EcInboundOrderLine();
        line.setOrderId(orderId);
        line.setSkuCode(skuCode);
        line.setQuantity(qty);
        line.setReceivedQuantity(received);
        return line;
    }

    private EcOutboundOrder outboundOrder(Long id, String status, String orderNo) {
        EcOutboundOrder o = new EcOutboundOrder();
        o.setId(id);
        o.setStatus(status);
        o.setOrderNo(orderNo);
        o.setOrderTime(LocalDateTime.of(2026, 8, 3, 10, 0));
        return o;
    }

    private EcOutboundOrderLine outboundLine(Long orderId, String skuCode, int qty, int shipped) {
        EcOutboundOrderLine line = new EcOutboundOrderLine();
        line.setOrderId(orderId);
        line.setSkuCode(skuCode);
        line.setQuantity(qty);
        line.setShippedQuantity(shipped);
        return line;
    }

    private EcFactory factory(Long id, String name) {
        EcFactory f = new EcFactory();
        f.setId(id);
        f.setName(name);
        return f;
    }

    private EcCarton carton(Long id, String name) {
        EcCarton c = new EcCarton();
        c.setId(id);
        c.setName(name);
        c.setLengthCm(new BigDecimal("30"));
        c.setWidthCm(new BigDecimal("20"));
        c.setHeightCm(new BigDecimal("10"));
        return c;
    }
}
