package com.ai.manager.system.service.support;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 电商图片默认命名：
 * <ul>
 *   <li>SKU 图：SPU名称-SKU名称.后缀</li>
 *   <li>SPU 主图：SPU名称-主图.后缀</li>
 *   <li>平台头像：平台名称-头像.后缀</li>
 *   <li>店铺头像：平台名称-店铺名称-头像.后缀</li>
 *   <li>快递头像：站点名称-头像.后缀</li>
 *   <li>纸箱预览：纸箱名称-预览.后缀</li>
 *   <li>未引用图片：未分类-yyyyMMdd-序号.后缀</li>
 * </ul>
 */
public final class EcEcommerceImageNameSupport {

    private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final String SPU_MAIN_SUFFIX = "主图";
    private static final String AVATAR_SUFFIX = "头像";
    private static final String CARTON_PREVIEW_SUFFIX = "预览";
    private static final String ORPHAN_PREFIX = "未分类";

    private EcEcommerceImageNameSupport() {
    }

    public static String resolveSkuDisplayName(String specName, String skuCode, Long skuId) {
        if (StringUtils.hasText(specName)) {
            return specName.trim();
        }
        if (StringUtils.hasText(skuCode)) {
            return skuCode.trim();
        }
        return skuId == null ? "SKU" : "SKU" + skuId;
    }

    public static String buildSkuImageFileName(String spuName, String skuDisplayName, String currentFileName) {
        return sanitizeSegment(spuName) + "-" + sanitizeSegment(skuDisplayName) + extractExtension(currentFileName);
    }

    public static String buildSpuMainImageFileName(String spuName, String currentFileName) {
        return sanitizeSegment(spuName) + "-" + SPU_MAIN_SUFFIX + extractExtension(currentFileName);
    }

    public static String buildPlatformAvatarFileName(String platformName, String currentFileName) {
        return sanitizeSegment(platformName) + "-" + AVATAR_SUFFIX + extractExtension(currentFileName);
    }

    public static String buildShopAvatarFileName(String platformName, String shopName, String currentFileName) {
        return sanitizeSegment(platformName) + "-" + sanitizeSegment(shopName) + "-" + AVATAR_SUFFIX
                + extractExtension(currentFileName);
    }

    public static String buildExpressAvatarFileName(String stationName, String currentFileName) {
        return sanitizeSegment(stationName) + "-" + AVATAR_SUFFIX + extractExtension(currentFileName);
    }

    public static String buildCartonPreviewFileName(String cartonName, String currentFileName) {
        return sanitizeSegment(cartonName) + "-" + CARTON_PREVIEW_SUFFIX + extractExtension(currentFileName);
    }

    public static String buildOrphanFileName(String dateKey, int sequence, String currentFileName) {
        String ext = extractExtension(currentFileName);
        return ORPHAN_PREFIX + "-" + sanitizeSegment(dateKey) + "-" + String.format("%03d", sequence) + ext;
    }

    public static String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ".jpg";
        }
        String trimmed = fileName.trim();
        int dot = trimmed.lastIndexOf('.');
        if (dot <= 0 || dot >= trimmed.length() - 1) {
            return ".jpg";
        }
        String ext = trimmed.substring(dot).toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.contains(ext) ? ext : ".jpg";
    }

    public static String sanitizeSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "未命名";
        }
        String sanitized = value.trim()
                .replace('\\', '-')
                .replace('/', '-');
        sanitized = INVALID_CHARS.matcher(sanitized).replaceAll("");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        while (sanitized.contains("--")) {
            sanitized = sanitized.replace("--", "-");
        }
        sanitized = trimEdgeDashes(sanitized);
        return sanitized.isEmpty() ? "未命名" : sanitized;
    }

    public static String allocateUniqueFileName(String baseName, Set<String> reservedNames) {
        String candidate = baseName;
        if (!reservedNames.contains(candidate.toLowerCase(Locale.ROOT))) {
            reservedNames.add(candidate.toLowerCase(Locale.ROOT));
            return candidate;
        }
        String ext = extractExtension(baseName);
        String stem = baseName.substring(0, baseName.length() - ext.length());
        int index = 2;
        while (true) {
            candidate = stem + "-" + index + ext;
            String key = candidate.toLowerCase(Locale.ROOT);
            if (!reservedNames.contains(key)) {
                reservedNames.add(key);
                return candidate;
            }
            index++;
        }
    }

    private static String trimEdgeDashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(start, end);
    }
}
