package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.vo.SysImportFieldVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class SysImportFieldRegistry {

    public static final String BIZ_SALES_ORDER = "SALES_ORDER";

    public static final String BIZ_SETTLEMENT_EXPRESS_BILL = "SETTLEMENT_EXPRESS_BILL";

    public static final String BIZ_EXPRESS_STATION_NAME = "EXPRESS_STATION_NAME";

    private SysImportFieldRegistry() {
    }

    public static boolean isScopeBased(String bizType) {
        return BIZ_SETTLEMENT_EXPRESS_BILL.equals(bizType) || BIZ_EXPRESS_STATION_NAME.equals(bizType);
    }

    public static boolean isExpressStationNameBiz(String bizType) {
        return BIZ_EXPRESS_STATION_NAME.equals(bizType);
    }

    public static List<SysImportFieldVO> listFields(String bizType) {
        if (BIZ_SALES_ORDER.equals(bizType)) {
            return salesOrderFields();
        }
        if (BIZ_SETTLEMENT_EXPRESS_BILL.equals(bizType)) {
            return settlementExpressBillFields();
        }
        return List.of();
    }

    private static List<SysImportFieldVO> settlementExpressBillFields() {
        List<SysImportFieldVO> list = new ArrayList<>();
        list.add(field("tracking_number", "运单号", "Tracking Number", true));
        list.add(field("freight_amount", "运费", "Freight Amount", true));
        list.add(field("settlement_destination", "结算目的地", "Settlement Destination", false));
        list.add(field("weight", "重量", "Weight", false));
        list.add(field("ship_time", "发货时间", "Ship Time", false));
        return list;
    }

    private static List<SysImportFieldVO> salesOrderFields() {
        List<SysImportFieldVO> list = new ArrayList<>();
        list.add(field("platform_order_no", "平台订单号", "Platform Order No", false));
        list.add(field("order_time", "下单时间", "Order Time", false));
        list.add(field("pay_time", "支付时间", "Pay Time", false));
        list.add(field("ship_time", "发货时间", "Ship Time", false));
        list.add(field("complete_time", "完成时间", "Complete Time", false));
        list.add(field("express_station_name", "快递站点", "Express Station", false));
        list.add(field("received_amount", "订单实收金额", "Received Amount", false));
        list.add(field("tracking_number", "快递单号", "Tracking Number", false));
        list.add(field("buyer_name", "买家", "Buyer Name", false));
        list.add(field("buyer_phone", "电话", "Buyer Phone", false));
        list.add(field("receive_address", "收货地址", "Receive Address", false));
        list.add(field("buyer_remark", "买家留言", "Buyer Remark", false));
        list.add(field("seller_remark", "卖家备注", "Seller Remark", false));
        list.add(field("link_name", "链接名称", "Link Name", true));
        list.add(field("sku_spec_name", "SKU规格名称", "SKU Spec Name", false));
        list.add(field("sku_quantity", "SKU数量", "SKU Quantity", false));
        list.add(field("platform_status", "平台订单状态", "Platform Order Status", false));
        list.add(field("platform_line_status", "子订单状态", "Line Status", false));
        return list;
    }

    private static SysImportFieldVO field(String key, String zh, String en, boolean required) {
        SysImportFieldVO vo = new SysImportFieldVO();
        vo.setKey(key);
        vo.setLabelZh(zh);
        vo.setLabelEn(en);
        vo.setRequired(required);
        return vo;
    }

    /**
     * 仅保留当前注册的系统字段，丢弃已下线的列映射项。
     */
    public static Map<String, String> sanitizeColumnMapping(String bizType, Map<String, String> saved) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>();
        for (SysImportFieldVO field : listFields(bizType)) {
            String value = saved != null ? saved.get(field.getKey()) : null;
            merged.put(field.getKey(), StringUtils.hasText(value) ? value.trim() : "");
        }
        return merged;
    }
}
