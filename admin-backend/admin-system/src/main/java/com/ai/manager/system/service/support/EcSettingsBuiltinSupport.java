package com.ai.manager.system.service.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EcSettingsBuiltinSupport {

    private EcSettingsBuiltinSupport() {
    }

    public static Map<String, String> defaultOrderImportStatusMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("交易成功", "COMPLETED");
        mapping.put("交易关闭", "CANCELLED");
        mapping.put("确认收货", "COMPLETED");
        mapping.put("卖家已发货，等待买家确认", "SHIPPED");
        mapping.put("等待买家确认收货", "SHIPPED");
        mapping.put("卖家已发货", "SHIPPED");
        mapping.put("等待买家确认", "SHIPPED");
        mapping.put("买家已付款，等待卖家发货", "PAID");
        mapping.put("买家已付款", "PAID");
        mapping.put("等待卖家发货", "PAID");
        mapping.put("待发货", "PAID");
        mapping.put("已关闭", "CANCELLED");
        mapping.put("已发货", "SHIPPED");
        mapping.put("已完成", "COMPLETED");
        mapping.put("已退款", "REFUNDED");
        mapping.put("退款成功", "REFUNDED");
        mapping.put("部分退款", "PARTIAL_REFUND");
        mapping.put("退款中", "REFUNDED");
        mapping.put("退货退款", "RETURNED");
        mapping.put("已取消", "CANCELLED");
        return mapping;
    }
}
