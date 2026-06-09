package com.ai.manager.system.domain.enums;

import lombok.Getter;

/** 平台/店铺渠道模式：线上电商 vs 线下门店/批发 */
@Getter
public enum EcChannelType {

    ONLINE("ONLINE", "线上", "Online"),
    OFFLINE("OFFLINE", "线下", "Offline");

    private final String code;
    private final String labelZh;
    private final String labelEn;

    EcChannelType(String code, String labelZh, String labelEn) {
        this.code = code;
        this.labelZh = labelZh;
        this.labelEn = labelEn;
    }

    public static EcChannelType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (EcChannelType value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        return null;
    }
}
