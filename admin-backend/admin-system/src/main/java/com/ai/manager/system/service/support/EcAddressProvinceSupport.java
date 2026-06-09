package com.ai.manager.system.service.support;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 从收货地址文本解析省份名称（与 ec_express_price.province_name 口径一致）。
 */
public final class EcAddressProvinceSupport {

    private static final List<String> PROVINCE_NAMES = Arrays.asList(
            "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
            "黑龙江省", "吉林省", "辽宁省", "河北省", "山西省", "江苏省", "浙江省", "安徽省", "福建省",
            "江西省", "山东省", "河南省", "湖北省", "湖南省", "广东省", "海南省", "四川省", "贵州省",
            "云南省", "陕西省", "甘肃省", "青海省", "台湾省",
            "北京市", "天津市", "上海市", "重庆市",
            "香港特别行政区", "澳门特别行政区"
    );

    private static final List<String> PROVINCE_NAMES_BY_LENGTH = PROVINCE_NAMES.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    private EcAddressProvinceSupport() {
    }

    /**
     * 从地址解析省份；地址为空或无法识别时返回 null。
     */
    public static String parseProvince(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        String text = address.trim();
        for (String name : PROVINCE_NAMES_BY_LENGTH) {
            if (text.startsWith(name)) {
                return name;
            }
        }
        int provinceIdx = text.indexOf('省');
        if (provinceIdx > 0 && provinceIdx <= 10) {
            return text.substring(0, provinceIdx + 1);
        }
        int autonomousIdx = text.indexOf("自治区");
        if (autonomousIdx > 0 && autonomousIdx <= 12) {
            return text.substring(0, autonomousIdx + 3);
        }
        return null;
    }

    /** 试算运费时使用的省份：订单解析省优先，否则店铺默认省。 */
    public static String resolveFreightProvince(String orderProvince, String shopDefaultProvince) {
        if (StringUtils.hasText(orderProvince)) {
            return orderProvince.trim();
        }
        if (StringUtils.hasText(shopDefaultProvince)) {
            return shopDefaultProvince.trim();
        }
        return EcExpressFeeSupport.DEFAULT_PROVINCE;
    }
}
