package com.ai.manager.system.service;

import org.springframework.util.StringUtils;

public interface EcEcommerceImageRenameService {

    String normalizeImageFileName(String currentValue, String desiredFileName);

    String normalizePlatformAvatar(String avatarUrl, String platformName);

    String normalizeShopAvatar(String avatarUrl, String platformName, String shopName);

    String normalizeExpressAvatar(String avatarUrl, String stationName);

    String normalizeCartonPreview(String previewImage, String cartonName);

    String normalizeSpuMainImage(String imageName, String spuName);

    String normalizeSkuImage(String imageName, String spuName, String specName, String skuCode, Long skuId);

    static String extractFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }
}
