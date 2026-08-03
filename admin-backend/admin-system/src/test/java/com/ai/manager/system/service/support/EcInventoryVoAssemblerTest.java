package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcInventory;
import com.ai.manager.system.domain.entity.EcInventoryLog;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.EcInventoryDetailVO;
import com.ai.manager.system.domain.vo.EcInventoryGlobalLogVO;
import com.ai.manager.system.domain.vo.EcInventoryInboundBriefVO;
import com.ai.manager.system.domain.vo.EcInventoryListItemVO;
import com.ai.manager.system.domain.vo.EcInventoryLogVO;
import com.ai.manager.system.domain.vo.EcInventoryOutboundBriefVO;
import com.ai.manager.system.domain.vo.EcInventoryPackingEstimateVO;
import com.ai.manager.system.service.support.EcInventoryVoAssembler.SkuBrief;
import com.ai.manager.system.service.support.EcInventoryVoAssembler.SkuContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EcInventoryVoAssembler 单元测试
 * 纯组装逻辑，直接构造实体与上下文断言 VO 字段映射（含预警态、日志截断、装箱估算）。
 */
class EcInventoryVoAssemblerTest {

    private final EcInventoryVoAssembler assembler = new EcInventoryVoAssembler();

    private EcInventory inventory(int qty, Integer threshold, Integer ignoreAlert) {
        EcInventory inv = new EcInventory();
        inv.setId(10L);
        inv.setSkuCode("SKU-001");
        inv.setQuantity(qty);
        inv.setAlertThreshold(threshold);
        inv.setIgnoreAlert(ignoreAlert);
        inv.setUpdateTime(LocalDateTime.of(2026, 8, 3, 10, 0));
        return inv;
    }

    private EcInventoryLog log(Long id) {
        EcInventoryLog log = new EcInventoryLog();
        log.setId(id);
        log.setInventoryId(10L);
        log.setChangeType("INBOUND");
        log.setChangeQty(3);
        log.setRefType("INBOUND_ORDER");
        log.setRefId(100L);
        log.setRemark("入库");
        log.setCreateTime(LocalDateTime.of(2026, 8, 3, 9, 0));
        return log;
    }

    private EcInventoryLogVO logVO() {
        return assembler.toLogVO(log(1L));
    }

    private SkuBrief brief() {
        SkuBrief b = new SkuBrief();
        b.specName = "红色/L";
        b.productName = "商品甲";
        b.productId = 7L;
        b.imageName = "img.png";
        b.salePrice = new BigDecimal("19.90");
        return b;
    }

    private SkuContext ctx() {
        SkuContext c = new SkuContext();
        c.skuId = 3L;
        c.productId = 7L;
        c.factoryId = 5L;
        c.factoryName = "工厂甲";
        c.specName = "红色/L";
        c.productName = "商品甲";
        c.skuStatus = "ON_SALE";
        c.productStatus = "ENABLED";
        c.salePrice = new BigDecimal("19.90");
        c.imageName = "img.png";
        c.unitsPerCarton = 10;
        c.cartonId = 2L;
        c.cartonName = "标准箱";
        c.cartonLengthCm = new BigDecimal("30");
        c.cartonWidthCm = new BigDecimal("20");
        c.cartonHeightCm = new BigDecimal("10");
        return c;
    }

    @Test
    void toListItemVO_shouldMapAllFieldsWithBrief() {
        EcInventoryListItemVO vo = assembler.toListItemVO(inventory(8, 5, 0), brief(), List.of(logVO()));

        assertThat(vo.getId()).isEqualTo(10L);
        assertThat(vo.getSkuCode()).isEqualTo("SKU-001");
        assertThat(vo.getSpecName()).isEqualTo("红色/L");
        assertThat(vo.getProductName()).isEqualTo("商品甲");
        assertThat(vo.getProductId()).isEqualTo(7L);
        assertThat(vo.getSalePrice()).isEqualByComparingTo(new BigDecimal("19.90"));
        assertThat(vo.getImageName()).isEqualTo("img.png");
        assertThat(vo.getQuantity()).isEqualTo(8);
        assertThat(vo.getIgnoreAlert()).isFalse();
        assertThat(vo.getAlertThreshold()).isEqualTo(5);
        assertThat(vo.getAlertActive()).isFalse();
        assertThat(vo.getRecentLogs()).hasSize(1);
    }

    @Test
    void toListItemVO_shouldTruncateRecentLogsBeyondLimit() {
        List<EcInventoryLogVO> logs = new ArrayList<>();
        for (long i = 1; i <= 7; i++) {
            logs.add(logVO());
        }
        EcInventoryListItemVO vo = assembler.toListItemVO(inventory(0, 0, 0), null, logs);
        // 超过 5 条截断为前 5 条；brief 为空时摘要字段不设置；库存 0 ≤ 阈值 0 预警激活
        assertThat(vo.getRecentLogs()).hasSize(5);
        assertThat(vo.getSpecName()).isNull();
        assertThat(vo.getAlertActive()).isTrue();
        assertThat(vo.getIgnoreAlert()).isFalse();
    }

    @Test
    void toLogVO_shouldMapAllFields() {
        EcInventoryLogVO vo = logVO();

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getInventoryId()).isEqualTo(10L);
        assertThat(vo.getChangeType()).isEqualTo("INBOUND");
        assertThat(vo.getChangeQty()).isEqualTo(3);
        assertThat(vo.getRefType()).isEqualTo("INBOUND_ORDER");
        assertThat(vo.getRefId()).isEqualTo(100L);
        assertThat(vo.getRemark()).isEqualTo("入库");
        assertThat(vo.getCreateTime()).isNotNull();
    }

    @Test
    void toGlobalLogVO_shouldMapLogAndContext() {
        EcInventoryGlobalLogVO vo = assembler.toGlobalLogVO(log(1L), inventory(8, 5, 0), ctx());

        assertThat(vo.getSkuCode()).isEqualTo("SKU-001");
        assertThat(vo.getSpecName()).isEqualTo("红色/L");
        assertThat(vo.getProductName()).isEqualTo("商品甲");
        assertThat(vo.getFactoryId()).isEqualTo(5L);
        assertThat(vo.getFactoryName()).isEqualTo("工厂甲");
        assertThat(vo.getChangeQty()).isEqualTo(3);
    }

    @Test
    void toGlobalLogVO_shouldTolerateNullInventoryAndContext() {
        EcInventoryGlobalLogVO vo = assembler.toGlobalLogVO(log(1L), null, null);

        assertThat(vo.getSkuCode()).isNull();
        assertThat(vo.getSpecName()).isNull();
        assertThat(vo.getChangeQty()).isEqualTo(3);
    }

    @Test
    void toDetailVO_shouldMapContextAndExternalData() {
        List<EcInventoryInboundBriefVO> inbound = List.of(new EcInventoryInboundBriefVO());
        List<EcInventoryOutboundBriefVO> outbound = List.of(new EcInventoryOutboundBriefVO());

        EcInventoryDetailVO vo = assembler.toDetailVO(inventory(8, 5, 0), ctx(), List.of(logVO()),
                3, inbound, outbound);

        assertThat(vo.getSkuCode()).isEqualTo("SKU-001");
        assertThat(vo.getSpecName()).isEqualTo("红色/L");
        assertThat(vo.getProductName()).isEqualTo("商品甲");
        assertThat(vo.getSkuId()).isEqualTo(3L);
        assertThat(vo.getFactoryId()).isEqualTo(5L);
        assertThat(vo.getFactoryName()).isEqualTo("工厂甲");
        assertThat(vo.getSkuStatus()).isEqualTo("ON_SALE");
        assertThat(vo.getImageName()).isEqualTo("img.png");
        assertThat(vo.getInTransitQty()).isEqualTo(3);
        assertThat(vo.getRelatedInboundOrders()).isSameAs(inbound);
        assertThat(vo.getRelatedOutboundOrders()).isSameAs(outbound);
        // 有上下文时给出装箱估算
        assertThat(vo.getPackingEstimate()).isNotNull();
        assertThat(vo.getPackingEstimate().getCartonsNeeded()).isEqualTo(1);
    }

    @Test
    void toDetailVO_shouldSkipContextDependentFieldsWhenContextNull() {
        EcInventoryDetailVO vo = assembler.toDetailVO(inventory(8, 5, 0), null, List.of(),
                0, List.of(), List.of());

        assertThat(vo.getSpecName()).isNull();
        assertThat(vo.getPackingEstimate()).isNull();
        assertThat(vo.getInTransitQty()).isZero();
        assertThat(vo.getRelatedInboundOrders()).isEmpty();
    }

    @Test
    void isAlertActive_shouldBeTrueWhenQuantityNotOverThreshold() {
        assertThat(assembler.isAlertActive(inventory(5, 5, 0))).isTrue();
        assertThat(assembler.isAlertActive(inventory(0, null, 0))).isTrue();
        assertThat(assembler.isAlertActive(inventory(6, 5, 0))).isFalse();
    }

    @Test
    void isAlertActive_shouldReturnFalseWhenIgnoreAlertFlagSet() {
        // 忽略预警标记下即使库存 ≤ 阈值也不激活
        assertThat(assembler.isAlertActive(inventory(0, 0, 1))).isFalse();
        assertThat(assembler.isAlertActive(inventory(5, 5, 1))).isFalse();
    }

    @Test
    void buildPackingEstimate_shouldReturnZeroCartonsWhenNonPositiveQty() {
        EcInventoryPackingEstimateVO vo = assembler.buildPackingEstimate(ctx(), 0);

        assertThat(vo.getOutboundQty()).isZero();
        assertThat(vo.getCartonsNeeded()).isZero();
        assertThat(vo.getTotalVolumeCm3()).isEqualByComparingTo(BigDecimal.ZERO);
        // 30*20*10 = 6000 cm³
        assertThat(vo.getCartonVolumeCm3()).isEqualByComparingTo(new BigDecimal("6000.00"));
    }

    @Test
    void buildPackingEstimate_shouldRoundUpCartonsAndComputeTotalVolume() {
        EcInventoryPackingEstimateVO vo = assembler.buildPackingEstimate(ctx(), 25);

        // 每箱 10 件，25 件需 3 箱（向上取整）
        assertThat(vo.getCartonsNeeded()).isEqualTo(3);
        assertThat(vo.getUnitsPerCarton()).isEqualTo(10);
        assertThat(vo.getCartonName()).isEqualTo("标准箱");
        assertThat(vo.getTotalVolumeCm3()).isEqualByComparingTo(new BigDecimal("18000.00"));
    }

    @Test
    void buildPackingEstimate_shouldDefaultUnitsPerCartonToOneWhenMissing() {
        SkuContext noCarton = ctx();
        noCarton.unitsPerCarton = null;
        noCarton.cartonLengthCm = null;

        EcInventoryPackingEstimateVO vo = assembler.buildPackingEstimate(noCarton, 5);

        assertThat(vo.getUnitsPerCarton()).isEqualTo(1);
        assertThat(vo.getCartonsNeeded()).isEqualTo(5);
        assertThat(vo.getCartonVolumeCm3()).isNull();
    }

    @Test
    void calcVolume_shouldReturnNullWhenAnyDimensionMissing() {
        assertThat(assembler.calcVolume(null, new BigDecimal("2"), new BigDecimal("3"))).isNull();
    }

    @Test
    void calcVolume_shouldScaleToTwoDecimals() {
        BigDecimal volume = assembler.calcVolume(
                new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.3"));
        assertThat(volume).isEqualByComparingTo(new BigDecimal("7.99"));
    }

    @Test
    void isSkuAvailableForInbound_shouldRequireOnSaleSkuAndEnabledProduct() {
        EcSku sku = sku("ON_SALE");
        EcProduct product = product("ENABLED");

        assertThat(assembler.isSkuAvailableForInbound(sku, product)).isTrue();
        assertThat(assembler.isSkuAvailableForInbound(null, product)).isFalse();
        assertThat(assembler.isSkuAvailableForInbound(sku("OFF_SALE"), product)).isFalse();
        assertThat(assembler.isSkuAvailableForInbound(sku, null)).isFalse();
        assertThat(assembler.isSkuAvailableForInbound(sku, product("DISABLED"))).isFalse();
    }

    private EcSku sku(String status) {
        EcSku sku = new EcSku();
        sku.setId(3L);
        sku.setStatus(status);
        return sku;
    }

    private EcProduct product(String status) {
        EcProduct product = new EcProduct();
        product.setId(7L);
        product.setStatus(status);
        return product;
    }
}
