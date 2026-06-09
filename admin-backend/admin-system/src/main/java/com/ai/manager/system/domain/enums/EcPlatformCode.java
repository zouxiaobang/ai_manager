package com.ai.manager.system.domain.enums;

import lombok.Getter;

/**
 * 电商平台标识，与 ec_platform.platform_code 一致。
 */
@Getter
public enum EcPlatformCode {

    OFFLINE(0, "线下", "Offline"),
    ALIBABA_1688(1, "1688", "1688"),
    TAOBAO(2, "淘宝", "Taobao"),
    TMALL(3, "天猫", "Tmall"),
    PINDUODUO(4, "拼多多", "Pinduoduo"),
    DOUYIN(5, "抖店", "Douyin Shop"),
    JD(6, "京东", "JD"),
    XIAOHONGSHU(7, "小红书", "Xiaohongshu"),
    KUAISHOU(8, "快手", "Kuaishou"),
    OTHER(99, "其他", "Other");

    private final int code;
    private final String labelZh;
    private final String labelEn;

    EcPlatformCode(int code, String labelZh, String labelEn) {
        this.code = code;
        this.labelZh = labelZh;
        this.labelEn = labelEn;
    }

    public static EcPlatformCode fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (EcPlatformCode value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
