package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSettlementExpressBill;
import com.ai.manager.system.domain.entity.EcSettlementExpressBillLine;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillImportVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillLineVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillRecordVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * 电商月结快递账单 VO 组装器
 *
 * <p>从 {@code EcMonthlySettlementServiceImpl} 中提取的纯组装逻辑：账单导入结果、账单行、
 * 账单记录、手工补录行等 VO 映射，以及纯字符串函数（快递站名、收货目的地拼接）。
 * 不访问数据库，数据由服务层加载后作为参数传入，便于独立单元测试。</p>
 */
@Component
public class EcExpressBillVoAssembler {

    /** 导入结果 VO：其他快递态下用统一占位 id 透传前台 */
    public EcSettlementExpressBillImportVO toImportVO(EcSettlementExpressBill bill, String stationName,
                                                      int manualPending) {
        EcSettlementExpressBillImportVO vo = new EcSettlementExpressBillImportVO();
        vo.setBillId(bill.getId());
        vo.setBillMonth(bill.getBillMonth());
        vo.setOtherExpress(Objects.equals(bill.getOtherExpress(), 1));
        vo.setExpressStationId(Objects.equals(bill.getOtherExpress(), 1)
                ? ExpressBillStationFilter.OTHER : bill.getExpressStationId());
        vo.setExpressStationName(stationName);
        vo.setTotalRows(bill.getTotalRows());
        vo.setMatchedRows(bill.getMatchedRows());
        vo.setUnmatchedRows(bill.getUnmatchedRows());
        vo.setGapOrderRows(bill.getGapOrderRows());
        vo.setManualPendingRows(manualPending);
        return vo;
    }

    /** 账单行 VO：逐字段映射，去除实体内部字段 */
    public EcSettlementExpressBillLineVO toLineVO(EcSettlementExpressBillLine line) {
        EcSettlementExpressBillLineVO vo = new EcSettlementExpressBillLineVO();
        vo.setId(line.getId());
        vo.setBillId(line.getBillId());
        vo.setExpressStationId(line.getExpressStationId());
        vo.setSource(line.getSource());
        vo.setOrderId(line.getOrderId());
        vo.setPlatformOrderNo(line.getPlatformOrderNo());
        vo.setOrderNo(line.getOrderNo());
        vo.setTrackingNumber(line.getTrackingNumber());
        vo.setFreightAmount(line.getFreightAmount());
        vo.setSettlementDestination(line.getSettlementDestination());
        vo.setWeight(line.getWeight());
        vo.setShipTime(line.getShipTime());
        vo.setMatchStatus(line.getMatchStatus());
        vo.setRemark(line.getRemark());
        return vo;
    }

    /** 手工补录行 VO：在行映射基础上补订单支付时间与店铺名（人工核对展示） */
    public EcSettlementExpressBillLineVO buildManualLineVO(EcSettlementExpressBillLine line,
                                                           Map<Long, EcSalesOrder> orderMap,
                                                           Map<Long, String> shopNameMap) {
        EcSettlementExpressBillLineVO vo = toLineVO(line);
        if (line.getOrderId() != null) {
            EcSalesOrder order = orderMap.get(line.getOrderId());
            if (order != null) {
                vo.setShipTime(order.getPayTime());
                if (order.getShopId() != null) {
                    vo.setShopName(shopNameMap.get(order.getShopId()));
                }
            }
        }
        return vo;
    }

    /** 缺口订单行 VO：无账单行的订单转展示行，含轨迹单号规整 */
    public EcSettlementExpressBillLineVO orderToLineVO(EcSalesOrder order, EcSettlementExpressBill bill,
                                                       Map<Long, String> shopNameMap) {
        EcSettlementExpressBillLineVO vo = new EcSettlementExpressBillLineVO();
        vo.setBillId(bill.getId());
        vo.setSource("GAP_ORDER");
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPlatformOrderNo(order.getPlatformOrderNo());
        vo.setTrackingNumber(order.getTrackingNumber() != null
                ? EcExpressBillParseSupport.normalizeTracking(order.getTrackingNumber()) : null);
        vo.setShipTime(order.getPayTime());
        vo.setSettlementDestination(resolveOrderDestination(order));
        vo.setMatchStatus("PENDING");
        if (order.getShopId() != null) {
            vo.setShopName(shopNameMap.get(order.getShopId()));
        }
        return vo;
    }

    /** 账单记录 VO：列表展示用，其他快递态给统一站名 */
    public EcSettlementExpressBillRecordVO toRecordVO(EcSettlementExpressBill bill, String stationName) {
        EcSettlementExpressBillRecordVO vo = new EcSettlementExpressBillRecordVO();
        vo.setId(bill.getId());
        vo.setBillMonth(bill.getBillMonth());
        vo.setOtherExpress(Objects.equals(bill.getOtherExpress(), 1));
        vo.setExpressStationId(Objects.equals(bill.getOtherExpress(), 1)
                ? ExpressBillStationFilter.OTHER : bill.getExpressStationId());
        vo.setExpressStationName(resolveBillStationName(bill, stationName));
        vo.setFileName(bill.getFileName());
        vo.setImportMode(bill.getImportMode());
        vo.setTotalRows(bill.getTotalRows());
        vo.setMatchedRows(bill.getMatchedRows());
        vo.setUnmatchedRows(bill.getUnmatchedRows());
        vo.setGapOrderRows(bill.getGapOrderRows());
        vo.setManualAppliedRows(bill.getManualAppliedRows());
        vo.setIncludeLabelPrice(Objects.equals(bill.getIncludeLabelPrice(), 1));
        vo.setStatus(bill.getStatus());
        vo.setCreateTime(bill.getCreateTime());
        return vo;
    }

    /** 快递站显示名：其他快递统一为占位名 */
    public String resolveBillStationName(EcSettlementExpressBill bill, String stationName) {
        if (Objects.equals(bill.getOtherExpress(), 1)) {
            return "其他快递公司";
        }
        return stationName;
    }

    /** 收货目的地拼接：省份 + 空格 + 城市，取 trim 后非空部分 */
    public String resolveOrderDestination(EcSalesOrder order) {
        if (order == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(order.getReceiveProvince())) {
            sb.append(order.getReceiveProvince().trim());
        }
        if (StringUtils.hasText(order.getReceiveCity())) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(order.getReceiveCity().trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
