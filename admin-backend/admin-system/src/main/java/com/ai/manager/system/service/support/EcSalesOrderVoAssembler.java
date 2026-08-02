package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSalesOrderLine;
import com.ai.manager.system.domain.entity.EcSalesOrderShortage;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.vo.EcSalesOrderDetailVO;
import com.ai.manager.system.domain.vo.EcSalesOrderLineVO;
import com.ai.manager.system.domain.vo.EcSalesOrderShortageVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 电商销售订单 VO 组装器
 *
 * <p>从 {@code EcSalesOrderServiceImpl} 中提取的纯组装逻辑（只做对象 → VO 字段映射，无数据访问），
 * 便于独立单元测试，也让订单服务主类保持职责单一。无状态，由 Spring 以单例注入。</p>
 */
@Component
public class EcSalesOrderVoAssembler {

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String MANUAL_DEFAULT_PLATFORM_STATUS = "已完成";

    /**
     * 组装订单详情 VO（含店铺/平台/站点名称、行明细与行数兜底）
     *
     * @param order          订单实体
     * @param lines          行明细 VO
     * @param shopMap        店铺 id → 店铺
     * @param platformMap    平台 id → 平台
     * @param stationNameMap 快递站点 id → 站点名
     * @param lineCount      行数（大于 0 时优先，否则取 lines.size()）
     */
    public EcSalesOrderDetailVO toDetailVO(EcSalesOrder order, List<EcSalesOrderLineVO> lines,
                                           Map<Long, EcShop> shopMap, Map<Long, EcPlatform> platformMap,
                                           Map<Long, String> stationNameMap, int lineCount) {
        EcSalesOrderDetailVO vo = new EcSalesOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setShopId(order.getShopId());
        EcShop shop = shopMap.get(order.getShopId());
        if (shop != null) {
            vo.setShopName(shop.getName());
            if (shop.getPlatformId() != null) {
                vo.setPlatformId(shop.getPlatformId());
                EcPlatform platform = platformMap.get(shop.getPlatformId());
                if (platform != null) {
                    vo.setPlatformName(platform.getName());
                }
            }
        }
        vo.setPlatformOrderNo(order.getPlatformOrderNo());
        vo.setSource(order.getSource());
        vo.setStatus(order.getStatus());
        vo.setPlatformStatus(resolvePlatformStatus(order));
        vo.setExpressStationId(order.getExpressStationId());
        if (order.getExpressStationId() != null) {
            vo.setExpressStationName(stationNameMap.get(order.getExpressStationId()));
        }
        vo.setOrderTime(order.getOrderTime());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setBuyerName(order.getBuyerName());
        vo.setBuyerPhone(order.getBuyerPhone());
        vo.setReceiveProvince(order.getReceiveProvince());
        vo.setReceiveCity(order.getReceiveCity());
        vo.setReceiveDistrict(order.getReceiveDistrict());
        vo.setReceiveAddress(order.getReceiveAddress());
        vo.setTrackingNumber(order.getTrackingNumber());
        vo.setBuyerRemark(order.getBuyerRemark());
        vo.setSellerRemark(order.getSellerRemark());
        vo.setReceivedAmount(order.getReceivedAmount());
        vo.setTotalCostAmount(order.getTotalCostAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setEstimatedFreightAmount(order.getEstimatedFreightAmount());
        vo.setActualFreightAmount(order.getActualFreightAmount());
        vo.setOrderCouponAmount(order.getOrderCouponAmount());
        vo.setPlatformFeeAmount(order.getPlatformFeeAmount());
        vo.setProfitAmount(order.getProfitAmount());
        vo.setTotalLossAmount(order.getTotalLossAmount());
        vo.setHasShortage(order.getHasShortage() != null && order.getHasShortage() == 1);
        vo.setImportBatchId(order.getImportBatchId());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setLines(lines);
        vo.setLineCount(lineCount > 0 ? lineCount : lines.size());
        return vo;
    }

    /**
     * 组装订单行 VO（逐字段映射，去除实体内部字段）
     */
    public EcSalesOrderLineVO toLineVO(EcSalesOrderLine line) {
        EcSalesOrderLineVO vo = new EcSalesOrderLineVO();
        vo.setId(line.getId());
        vo.setOrderId(line.getOrderId());
        vo.setSortOrder(line.getSortOrder());
        vo.setListingLinkSkuId(line.getListingLinkSkuId());
        vo.setLinkName(line.getLinkName());
        vo.setSkuSpecName(line.getSkuSpecName());
        vo.setSkuCodes(line.getSkuCodes());
        vo.setSkuQuantity(line.getSkuQuantity());
        vo.setShippedQuantity(line.getShippedQuantity());
        vo.setShortQuantity(line.getShortQuantity());
        vo.setStatus(line.getStatus());
        vo.setPlatformLineStatus(line.getPlatformLineStatus());
        vo.setRefundType(line.getRefundType());
        vo.setRefundTime(line.getRefundTime());
        vo.setRefundAmount(line.getRefundAmount());
        vo.setLossAmount(line.getLossAmount());
        vo.setUnitPrice(line.getUnitPrice());
        vo.setDiscountPct(line.getDiscountPct());
        vo.setLineCouponAmount(line.getLineCouponAmount());
        vo.setLineReceivedAmount(line.getLineReceivedAmount());
        vo.setSkuAmount(line.getSkuAmount());
        vo.setCartonAmount(line.getCartonAmount());
        vo.setExpressAmount(line.getExpressAmount());
        vo.setBaseCostAmount(line.getBaseCostAmount());
        vo.setPlatformFeeAmount(line.getPlatformFeeAmount());
        vo.setCostPrice(line.getCostPrice());
        vo.setMinSetAmount(line.getMinSetAmount());
        vo.setProfit(line.getProfit());
        vo.setPricingRisk(line.getPricingRisk());
        vo.setPlatformLineNo(line.getPlatformLineNo());
        vo.setPlatformItemName(line.getPlatformItemName());
        return vo;
    }

    /**
     * 组装缺货记录 VO（逐字段映射）
     */
    public EcSalesOrderShortageVO toShortageVO(EcSalesOrderShortage s) {
        EcSalesOrderShortageVO vo = new EcSalesOrderShortageVO();
        vo.setId(s.getId());
        vo.setSkuCode(s.getSkuCode());
        vo.setNeedQty(s.getNeedQty());
        vo.setDeductedQty(s.getDeductedQty());
        vo.setShortQty(s.getShortQty());
        vo.setStatus(s.getStatus());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }

    /**
     * 推断平台状态：优先取订单已存的平台状态；手动订单无记录时回退默认「已完成」
     */
    private String resolvePlatformStatus(EcSalesOrder order) {
        if (StringUtils.hasText(order.getPlatformStatus())) {
            return order.getPlatformStatus().trim();
        }
        if (SOURCE_MANUAL.equals(order.getSource())) {
            return MANUAL_DEFAULT_PLATFORM_STATUS;
        }
        return null;
    }
}
