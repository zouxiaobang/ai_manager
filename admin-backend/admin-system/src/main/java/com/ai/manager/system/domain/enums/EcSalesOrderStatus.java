package com.ai.manager.system.domain.enums;

import lombok.Getter;

/**
 * 销售订单头状态，与 ec_sales_order.status 一致。
 * 部分退款：同单存在已退款/已退货明细且仍有未退款有效明细时由明细聚合得出。
 */
@Getter
public enum EcSalesOrderStatus {

    DRAFT("DRAFT", "草稿", "Draft"),
    PAID("PAID", "待发货", "Paid"),
    PARTIAL_SHIPPED("PARTIAL_SHIPPED", "部分发货", "Partial Shipped"),
    SHIPPED("SHIPPED", "已发货", "Shipped"),
    PARTIAL_REFUND("PARTIAL_REFUND", "部分退款", "Partial Refund"),
    COMPLETED("COMPLETED", "已完成", "Completed"),
    CANCELLED("CANCELLED", "已取消", "Cancelled"),
    REFUNDED("REFUNDED", "已退款", "Refunded");

    private final String code;
    private final String labelZh;
    private final String labelEn;

    EcSalesOrderStatus(String code, String labelZh, String labelEn) {
        this.code = code;
        this.labelZh = labelZh;
        this.labelEn = labelEn;
    }

    public static EcSalesOrderStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        for (EcSalesOrderStatus value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}
