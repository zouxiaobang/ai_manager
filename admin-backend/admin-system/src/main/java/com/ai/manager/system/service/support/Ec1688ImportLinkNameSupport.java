package com.ai.manager.system.service.support;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1688 订单导入：从「货品标题」等合并字段中解析链接名称与 SKU 规格。
 * <p>
 * 示例：「儿童玩具 颜色: 独角兽变形弹射车（英文版）」→ 链接名「儿童玩具」，规格「独角兽变形弹射车（英文版）」
 */
public final class Ec1688ImportLinkNameSupport {

    /**
     * 1688 导出常见格式：{链接名称} {属性标签}: {规格值}，属性标签如「颜色」「规格型号」等。
     */
    private static final Pattern COMBINED_TITLE = Pattern.compile(
            "^(.*)\\s+([\\u4e00-\\u9fa5a-zA-Z0-9]{2,12})\\s*[:：]\\s*(.+)$");

    private Ec1688ImportLinkNameSupport() {
    }

    public record ParsedLinkSku(String linkName, String skuSpecName) {
    }

    /**
     * 解析合并标题。无法识别「属性: 规格」后缀时，整段作为链接名称，规格为空。
     */
    public static ParsedLinkSku parse(String combined) {
        if (!StringUtils.hasText(combined)) {
            return new ParsedLinkSku(null, null);
        }
        String trimmed = combined.trim();
        Matcher matcher = COMBINED_TITLE.matcher(trimmed);
        if (matcher.matches()) {
            String linkName = matcher.group(1).trim();
            String skuSpecName = matcher.group(3).trim();
            if (StringUtils.hasText(linkName) && StringUtils.hasText(skuSpecName)) {
                return new ParsedLinkSku(linkName, skuSpecName);
            }
        }
        return new ParsedLinkSku(trimmed, null);
    }
}
