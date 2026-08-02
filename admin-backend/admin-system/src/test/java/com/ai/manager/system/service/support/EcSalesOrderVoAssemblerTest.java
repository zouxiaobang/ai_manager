package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSalesOrderLine;
import com.ai.manager.system.domain.entity.EcSalesOrderShortage;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.vo.EcSalesOrderDetailVO;
import com.ai.manager.system.domain.vo.EcSalesOrderLineVO;
import com.ai.manager.system.domain.vo.EcSalesOrderShortageVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EcSalesOrderVoAssembler 单元测试
 * 纯组装逻辑，直接构造实体断言 VO 字段映射（含店铺/平台名、平台状态推断与行数兜底）。
 */
class EcSalesOrderVoAssemblerTest {

    private final EcSalesOrderVoAssembler assembler = new EcSalesOrderVoAssembler();

    private EcSalesOrderLine line(Long id) {
        EcSalesOrderLine l = new EcSalesOrderLine();
        l.setId(id);
        l.setOrderId(100L);
        l.setSortOrder(1);
        l.setLinkName("链接名");
        l.setSkuSpecName("红色/L");
        l.setSkuQuantity(2);
        l.setStatus("PAID");
        l.setUnitPrice(new BigDecimal("9.90"));
        l.setPlatformLineNo("PL-001");
        return l;
    }

    @Test
    void toLineVO_shouldMapAllFields() {
        EcSalesOrderLineVO vo = assembler.toLineVO(line(1L));

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getOrderId()).isEqualTo(100L);
        assertThat(vo.getSortOrder()).isEqualTo(1);
        assertThat(vo.getLinkName()).isEqualTo("链接名");
        assertThat(vo.getSkuSpecName()).isEqualTo("红色/L");
        assertThat(vo.getSkuQuantity()).isEqualTo(2);
        assertThat(vo.getStatus()).isEqualTo("PAID");
        assertThat(vo.getUnitPrice()).isEqualByComparingTo(new BigDecimal("9.90"));
        assertThat(vo.getPlatformLineNo()).isEqualTo("PL-001");
        // 行 VO 不应暴露实体的 deleted 字段
        assertThat(vo.getClass().getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("deleted");
    }

    @Test
    void toShortageVO_shouldMapFields() {
        EcSalesOrderShortage s = new EcSalesOrderShortage();
        s.setId(9L);
        s.setSkuCode("SKU-1");
        s.setNeedQty(5);
        s.setDeductedQty(3);
        s.setShortQty(2);
        s.setStatus("PENDING");
        s.setCreateTime(LocalDateTime.of(2026, 8, 3, 10, 0));

        EcSalesOrderShortageVO vo = assembler.toShortageVO(s);

        assertThat(vo.getId()).isEqualTo(9L);
        assertThat(vo.getSkuCode()).isEqualTo("SKU-1");
        assertThat(vo.getNeedQty()).isEqualTo(5);
        assertThat(vo.getDeductedQty()).isEqualTo(3);
        assertThat(vo.getShortQty()).isEqualTo(2);
        assertThat(vo.getStatus()).isEqualTo("PENDING");
        assertThat(vo.getCreateTime()).isEqualTo(s.getCreateTime());
    }

    private EcSalesOrder order(String source, String platformStatus) {
        EcSalesOrder o = new EcSalesOrder();
        o.setId(100L);
        o.setOrderNo("SO-100");
        o.setShopId(1L);
        o.setPlatformOrderNo("PO-100");
        o.setSource(source);
        o.setStatus("PAID");
        o.setPlatformStatus(platformStatus);
        o.setExpressStationId(5L);
        o.setBuyerName("张三");
        o.setHasShortage(1);
        o.setActualFreightAmount(new BigDecimal("10.00"));
        o.setProfitAmount(new BigDecimal("50.00"));
        return o;
    }

    private EcSalesOrderDetailVO assemble(EcSalesOrder order) {
        EcShop shop = new EcShop();
        shop.setId(1L);
        shop.setName("天猫旗舰店");
        shop.setPlatformId(2L);
        EcPlatform platform = new EcPlatform();
        platform.setId(2L);
        platform.setName("天猫");
        return assembler.toDetailVO(order, List.of(), Map.of(1L, shop), Map.of(2L, platform),
                Map.of(5L, "广州站"), 3);
    }

    @Test
    void toDetailVO_shouldResolveShopAndPlatformNames() {
        EcSalesOrderDetailVO vo = assemble(order("MANUAL", null));

        assertThat(vo.getId()).isEqualTo(100L);
        assertThat(vo.getOrderNo()).isEqualTo("SO-100");
        assertThat(vo.getShopName()).isEqualTo("天猫旗舰店");
        assertThat(vo.getPlatformId()).isEqualTo(2L);
        assertThat(vo.getPlatformName()).isEqualTo("天猫");
        assertThat(vo.getExpressStationName()).isEqualTo("广州站");
        assertThat(vo.getBuyerName()).isEqualTo("张三");
        // hasShortage=1 → 布尔 true
        assertThat(vo.getHasShortage()).isTrue();
        assertThat(vo.getActualFreightAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        // lineCount 参数 >0 优先
        assertThat(vo.getLineCount()).isEqualTo(3);
    }

    @Test
    void toDetailVO_lineCountFallback_shouldUseLinesSize() {
        EcSalesOrderDetailVO vo = assembler.toDetailVO(order("MANUAL", null), List.of(),
                Map.of(), Map.of(), Map.of(), 0);

        assertThat(vo.getLineCount()).isZero();
    }

    @Test
    void toDetailVO_platformStatus_withExistingValue_shouldTrimReturn() {
        EcSalesOrderDetailVO vo = assemble(order("IMPORT", " 已发货 "));

        assertThat(vo.getPlatformStatus()).isEqualTo("已发货");
    }

    @Test
    void toDetailVO_platformStatus_manualWithoutValue_shouldFallbackDefault() {
        EcSalesOrderDetailVO vo = assemble(order("MANUAL", null));

        assertThat(vo.getPlatformStatus()).isEqualTo("已完成");
    }

    @Test
    void toDetailVO_platformStatus_importWithoutValue_shouldReturnNull() {
        EcSalesOrderDetailVO vo = assemble(order("IMPORT", null));

        assertThat(vo.getPlatformStatus()).isNull();
    }
}
