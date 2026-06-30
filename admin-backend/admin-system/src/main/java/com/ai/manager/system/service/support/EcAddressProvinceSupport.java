package com.ai.manager.system.service.support;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final Map<String, String> PROVINCE_ALIASES = buildProvinceAliases();

    private EcAddressProvinceSupport() {
    }

    private static Map<String, String> buildProvinceAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("内蒙", "内蒙古自治区");
        aliases.put("内蒙古", "内蒙古自治区");
        aliases.put("广西", "广西壮族自治区");
        aliases.put("广西壮族", "广西壮族自治区");
        aliases.put("宁夏", "宁夏回族自治区");
        aliases.put("宁夏回族", "宁夏回族自治区");
        aliases.put("新疆", "新疆维吾尔自治区");
        aliases.put("新疆维吾尔", "新疆维吾尔自治区");
        aliases.put("西藏", "西藏自治区");
        aliases.put("香港", "香港特别行政区");
        aliases.put("澳门", "澳门特别行政区");
        aliases.put("台", "台湾省");
        aliases.put("台湾", "台湾省");
        return aliases;
    }

    /**
     * 从地址解析省份；地址为空或无法识别时返回 null。
     */
    public static String parseProvince(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        String text = sanitize(address);
        for (String name : PROVINCE_NAMES_BY_LENGTH) {
            if (text.startsWith(name)) {
                return name;
            }
        }
        int provinceIdx = text.indexOf('省');
        if (provinceIdx > 0 && provinceIdx <= 10) {
            String candidate = text.substring(0, provinceIdx + 1);
            if (PROVINCE_NAMES.contains(candidate)) {
                return candidate;
            }
        }
        int autonomousIdx = text.indexOf("自治区");
        if (autonomousIdx > 0 && autonomousIdx <= 12) {
            String candidate = text.substring(0, autonomousIdx + 3);
            if (PROVINCE_NAMES.contains(candidate)) {
                return candidate;
            }
        }
        int sarIdx = text.indexOf("特别行政区");
        if (sarIdx > 0 && sarIdx <= 8) {
            String candidate = text.substring(0, sarIdx + 5);
            if (PROVINCE_NAMES.contains(candidate)) {
                return candidate;
            }
        }
        return normalizeProvinceName(text);
    }

    /** 试算运费时使用的省份：订单解析省优先，否则店铺默认省。 */
    public static String resolveFreightProvince(String orderProvince, String shopDefaultProvince) {
        if (StringUtils.hasText(orderProvince)) {
            String normalized = normalizeProvinceName(orderProvince);
            if (normalized != null) {
                return normalized;
            }
        }
        if (StringUtils.hasText(shopDefaultProvince)) {
            String normalized = normalizeProvinceName(shopDefaultProvince);
            if (normalized != null) {
                return normalized;
            }
        }
        return EcExpressFeeSupport.DEFAULT_PROVINCE;
    }

    /**
     * 规范化为标准省份全称；无法识别时返回清洗后的原文。
     */
    public static String canonicalProvinceName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = normalizeProvinceName(raw);
        return normalized != null ? normalized : sanitize(raw);
    }

    /** 判断两个省份名称是否指向同一地区（兼容 广东 / 广东省 等写法）。 */
    public static boolean provinceNamesEquivalent(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return false;
        }
        String ca = canonicalProvinceName(a);
        String cb = canonicalProvinceName(b);
        return ca != null && ca.equals(cb);
    }

    /**
     * 规范化为与 ec_express_price.province_name 一致的省份全称。
     * 支持简称智能匹配，例如：广东→广东省、北京→北京市、内蒙→内蒙古自治区。
     * 无法识别时返回 null，由调用方回退默认省。
     */
    public static String normalizeProvinceName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String text = sanitize(raw);
        if (PROVINCE_NAMES.contains(text)) {
            return text;
        }
        String alias = PROVINCE_ALIASES.get(text);
        if (alias != null) {
            return alias;
        }
        String parsed = parseProvinceFromKnownPrefixes(text);
        if (parsed != null) {
            return parsed;
        }
        String withSuffix = matchByAppendingSuffix(text);
        if (withSuffix != null) {
            return withSuffix;
        }
        String byCoreName = matchByCoreName(text);
        if (byCoreName != null) {
            return byCoreName;
        }
        return matchUniquePrefix(text);
    }

    private static String sanitize(String raw) {
        return raw.trim()
                .replace('　', ' ')
                .replaceAll("\\s+", "");
    }

    private static String parseProvinceFromKnownPrefixes(String text) {
        for (String name : PROVINCE_NAMES_BY_LENGTH) {
            if (text.startsWith(name)) {
                return name;
            }
        }
        return null;
    }

    private static String matchByAppendingSuffix(String text) {
        if (text.endsWith("省")
                || text.endsWith("市")
                || text.contains("自治区")
                || text.contains("特别行政区")) {
            return null;
        }
        String[] suffixes = {"省", "市", "自治区", "特别行政区"};
        for (String suffix : suffixes) {
            String candidate = text + suffix;
            if (PROVINCE_NAMES.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String matchByCoreName(String text) {
        List<String> matches = PROVINCE_NAMES.stream()
                .filter(name -> text.equals(stripProvinceSuffix(name)))
                .collect(Collectors.toList());
        if (matches.size() == 1) {
            return matches.get(0);
        }
        return null;
    }

    private static String matchUniquePrefix(String text) {
        if (text.length() < 2) {
            return null;
        }
        List<String> matches = PROVINCE_NAMES.stream()
                .filter(name -> name.startsWith(text))
                .collect(Collectors.toList());
        if (matches.size() == 1) {
            return matches.get(0);
        }
        return null;
    }

    private static String stripProvinceSuffix(String provinceName) {
        if (provinceName.endsWith("特别行政区")) {
            return provinceName.substring(0, provinceName.length() - 5);
        }
        if (provinceName.endsWith("自治区")) {
            String core = provinceName.substring(0, provinceName.indexOf("自治区"));
            int ethnicIdx = core.indexOf("壮族");
            if (ethnicIdx > 0) {
                return core.substring(0, ethnicIdx);
            }
            ethnicIdx = core.indexOf("回族");
            if (ethnicIdx > 0) {
                return core.substring(0, ethnicIdx);
            }
            ethnicIdx = core.indexOf("维吾尔");
            if (ethnicIdx > 0) {
                return core.substring(0, ethnicIdx);
            }
            return core;
        }
        if (provinceName.endsWith("省") || provinceName.endsWith("市")) {
            return provinceName.substring(0, provinceName.length() - 1);
        }
        return provinceName;
    }
}
