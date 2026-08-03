package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSettlementExpressBill;
import com.ai.manager.system.domain.entity.EcSettlementExpressBillLine;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillImportVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillLineVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillRecordVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EcExpressBillVoAssembler 单元测试
 * 纯 VO 映射，直接构造账单/行/订单实体断言导入结果、账单行、账单记录与目的地拼接。
 */
class EcExpressBillVoAssemblerTest {

    private final EcExpressBillVoAssembler assembler = new EcExpressBillVoAssembler();

    private EcSettlementExpressBill bill(Long id, int otherExpress, String fileName) {
        EcSettlementExpressBill bill = new EcSettlementExpressBill();
        bill.setId(id);
        bill.setBillMonth("2026-08");
        bill.setOtherExpress(otherExpress);
        bill.setExpressStationId(5L);
        bill.setFileName(fileName);
        bill.setImportMode("FILE");
        bill.setTotalRows(10);
        bill.setMatchedRows(7);
        bill.setUnmatchedRows(3);
        bill.setGapOrderRows(2);
        bill.setManualAppliedRows(1);
        bill.setIncludeLabelPrice(1);
        bill.setStatus("IMPORTED");
        bill.setCreateTime(LocalDateTime.of(2026, 8, 3, 10, 0));
        return bill;
    }

    private EcSettlementExpressBillLine line(Long id, String source, String matchStatus) {
        EcSettlementExpressBillLine line = new EcSettlementExpressBillLine();
        line.setId(id);
        line.setBillId(9L);
        line.setExpressStationId(5L);
        line.setSource(source);
        line.setOrderId(100L);
        line.setOrderNo("NO-100");
        line.setPlatformOrderNo("PLAT-100");
        line.setTrackingNumber("SF123");
        line.setFreightAmount(new BigDecimal("12.50"));
        line.setSettlementDestination("浙江省 杭州市");
        line.setWeight(new BigDecimal("1.50"));
        line.setShipTime(LocalDateTime.of(2026, 8, 3, 9, 0));
        line.setMatchStatus(matchStatus);
        line.setRemark("备注");
        return line;
    }

    private EcSalesOrder order(Long id) {
        EcSalesOrder order = new EcSalesOrder();
        order.setId(id);
        order.setOrderNo("NO-" + id);
        order.setPlatformOrderNo("PLAT-" + id);
        order.setTrackingNumber("SF123 ");
        order.setShopId(7L);
        order.setPayTime(LocalDateTime.of(2026, 8, 3, 8, 30));
        order.setReceiveProvince(" 浙江省 ");
        order.setReceiveCity(" 杭州市 ");
        return order;
    }

    @Test
    void toImportVO_shouldMapAllFieldsForNormalStation() {
        EcSettlementExpressBillImportVO vo = assembler.toImportVO(bill(9L, 0, "a.xlsx"), "顺丰", 4);

        assertThat(vo.getBillId()).isEqualTo(9L);
        assertThat(vo.getBillMonth()).isEqualTo("2026-08");
        assertThat(vo.getOtherExpress()).isFalse();
        assertThat(vo.getExpressStationId()).isEqualTo(5L);
        assertThat(vo.getExpressStationName()).isEqualTo("顺丰");
        assertThat(vo.getTotalRows()).isEqualTo(10);
        assertThat(vo.getMatchedRows()).isEqualTo(7);
        assertThat(vo.getUnmatchedRows()).isEqualTo(3);
        assertThat(vo.getGapOrderRows()).isEqualTo(2);
        assertThat(vo.getManualPendingRows()).isEqualTo(4);
    }

    @Test
    void toImportVO_shouldUseOtherPlaceholderIdWhenOtherExpress() {
        EcSettlementExpressBillImportVO vo = assembler.toImportVO(bill(9L, 1, "a.xlsx"), "其他快递公司", 0);

        assertThat(vo.getOtherExpress()).isTrue();
        assertThat(vo.getExpressStationId()).isEqualTo(ExpressBillStationFilter.OTHER);
    }

    @Test
    void toLineVO_shouldMapAllFields() {
        EcSettlementExpressBillLineVO vo = assembler.toLineVO(line(1L, "FILE", "MATCHED"));

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getBillId()).isEqualTo(9L);
        assertThat(vo.getExpressStationId()).isEqualTo(5L);
        assertThat(vo.getSource()).isEqualTo("FILE");
        assertThat(vo.getOrderId()).isEqualTo(100L);
        assertThat(vo.getOrderNo()).isEqualTo("NO-100");
        assertThat(vo.getPlatformOrderNo()).isEqualTo("PLAT-100");
        assertThat(vo.getTrackingNumber()).isEqualTo("SF123");
        assertThat(vo.getFreightAmount()).isEqualByComparingTo("12.50");
        assertThat(vo.getSettlementDestination()).isEqualTo("浙江省 杭州市");
        assertThat(vo.getMatchStatus()).isEqualTo("MATCHED");
        assertThat(vo.getShipTime()).isNotNull();
    }

    @Test
    void buildManualLineVO_shouldEnrichShipTimeAndShopName() {
        EcSettlementExpressBillLine line = line(1L, "MANUAL", "PENDING");
        EcSalesOrder order = order(100L);
        EcSettlementExpressBillLineVO vo = assembler.buildManualLineVO(
                line, Map.of(100L, order), Map.of(7L, "店铺甲"));

        assertThat(vo.getShipTime()).isEqualTo(order.getPayTime());
        assertThat(vo.getShopName()).isEqualTo("店铺甲");
        assertThat(vo.getSource()).isEqualTo("MANUAL");
    }

    @Test
    void orderToLineVO_shouldBuildGapOrderRowWithNormalizedTracking() {
        EcSettlementExpressBill bill = bill(9L, 0, null);
        EcSettlementExpressBillLineVO vo = assembler.orderToLineVO(order(100L), bill, Map.of(7L, "店铺甲"));

        assertThat(vo.getBillId()).isEqualTo(9L);
        assertThat(vo.getSource()).isEqualTo("GAP_ORDER");
        assertThat(vo.getOrderId()).isEqualTo(100L);
        assertThat(vo.getTrackingNumber()).isEqualTo("SF123");
        assertThat(vo.getSettlementDestination()).isEqualTo("浙江省 杭州市");
        assertThat(vo.getMatchStatus()).isEqualTo("PENDING");
        assertThat(vo.getShopName()).isEqualTo("店铺甲");
    }

    @Test
    void toRecordVO_shouldMapBillRecordFields() {
        EcSettlementExpressBillRecordVO vo = assembler.toRecordVO(bill(9L, 0, "a.xlsx"), "顺丰");

        assertThat(vo.getId()).isEqualTo(9L);
        assertThat(vo.getFileName()).isEqualTo("a.xlsx");
        assertThat(vo.getImportMode()).isEqualTo("FILE");
        assertThat(vo.getTotalRows()).isEqualTo(10);
        assertThat(vo.getManualAppliedRows()).isEqualTo(1);
        assertThat(vo.getIncludeLabelPrice()).isTrue();
        assertThat(vo.getExpressStationName()).isEqualTo("顺丰");
    }

    @Test
    void toRecordVO_shouldUsePlaceholderStationNameWhenOtherExpress() {
        EcSettlementExpressBillRecordVO vo = assembler.toRecordVO(bill(9L, 1, "a.xlsx"), null);
        assertThat(vo.getExpressStationName()).isEqualTo("其他快递公司");
    }

    @Test
    void resolveBillStationName_shouldReturnPlaceholderForOtherExpress() {
        assertThat(assembler.resolveBillStationName(bill(9L, 1, null), "顺丰")).isEqualTo("其他快递公司");
        assertThat(assembler.resolveBillStationName(bill(9L, 0, null), "顺丰")).isEqualTo("顺丰");
    }

    @Test
    void resolveOrderDestination_shouldJoinProvinceAndCityWithTrim() {
        assertThat(assembler.resolveOrderDestination(order(100L))).isEqualTo("浙江省 杭州市");
    }

    @Test
    void resolveOrderDestination_shouldReturnNullWhenNoRegion() {
        EcSalesOrder order = order(100L);
        order.setReceiveProvince(null);
        order.setReceiveCity("");
        assertThat(assembler.resolveOrderDestination(order)).isNull();
        assertThat(assembler.resolveOrderDestination(null)).isNull();
    }
}
