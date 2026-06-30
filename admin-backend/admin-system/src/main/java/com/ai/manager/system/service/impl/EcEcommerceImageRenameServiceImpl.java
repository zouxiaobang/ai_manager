package com.ai.manager.system.service.impl;

import com.ai.manager.system.service.EcEcommerceImageRenameService;
import com.ai.manager.system.service.ImageSpaceService;
import com.ai.manager.system.service.support.EcEcommerceImageNameSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EcEcommerceImageRenameServiceImpl implements EcEcommerceImageRenameService {

    private final ImageSpaceService imageSpaceService;

    @Override
    public String normalizeImageFileName(String currentValue, String desiredFileName) {
        if (!StringUtils.hasText(currentValue) || !StringUtils.hasText(desiredFileName)) {
            return trimToNull(currentValue);
        }
        String oldName = EcEcommerceImageRenameService.extractFileName(currentValue);
        if (!StringUtils.hasText(oldName)) {
            return trimToNull(currentValue);
        }
        if (oldName.equalsIgnoreCase(desiredFileName.trim())) {
            return oldName;
        }
        return imageSpaceService.renameEcommerceFileName(oldName, desiredFileName.trim());
    }

    @Override
    public String normalizePlatformAvatar(String avatarUrl, String platformName) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }
        return normalizeImageFileName(
                avatarUrl,
                EcEcommerceImageNameSupport.buildPlatformAvatarFileName(platformName, avatarUrl)
        );
    }

    @Override
    public String normalizeShopAvatar(String avatarUrl, String platformName, String shopName) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }
        return normalizeImageFileName(
                avatarUrl,
                EcEcommerceImageNameSupport.buildShopAvatarFileName(platformName, shopName, avatarUrl)
        );
    }

    @Override
    public String normalizeExpressAvatar(String avatarUrl, String stationName) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }
        return normalizeImageFileName(
                avatarUrl,
                EcEcommerceImageNameSupport.buildExpressAvatarFileName(stationName, avatarUrl)
        );
    }

    @Override
    public String normalizeCartonPreview(String previewImage, String cartonName) {
        if (!StringUtils.hasText(previewImage)) {
            return null;
        }
        return normalizeImageFileName(
                previewImage,
                EcEcommerceImageNameSupport.buildCartonPreviewFileName(cartonName, previewImage)
        );
    }

    @Override
    public String normalizeSpuMainImage(String imageName, String spuName) {
        if (!StringUtils.hasText(imageName)) {
            return null;
        }
        return normalizeImageFileName(
                imageName,
                EcEcommerceImageNameSupport.buildSpuMainImageFileName(spuName, imageName)
        );
    }

    @Override
    public String normalizeSkuImage(String imageName, String spuName, String specName, String skuCode, Long skuId) {
        if (!StringUtils.hasText(imageName)) {
            return null;
        }
        String skuDisplayName = EcEcommerceImageNameSupport.resolveSkuDisplayName(specName, skuCode, skuId);
        return normalizeImageFileName(
                imageName,
                EcEcommerceImageNameSupport.buildSkuImageFileName(spuName, skuDisplayName, imageName)
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
