package com.ai.manager.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "ai-manager.baidu-pan")
public class BaiduPanProperties {

    /** 百度开放平台 AppKey / client_id */
    private String appKey = "";

    /** 百度开放平台 SecretKey / client_secret */
    private String secretKey = "";

    /** 网盘应用目录名，对应 /apps/{appFolderName}/ */
    private String appFolderName = "ai_manager";

    /**
     * 环境子目录：开发环境填 dev，文件写入 /apps/{appFolderName}/dev/...
     * 生产环境留空（或 prod），沿用 /apps/{appFolderName}/... 以兼容历史数据。
     */
    private String environmentFolder = "";

    /** OAuth 回调地址，需在百度开放平台配置一致 */
    private String redirectUri = "http://127.0.0.1:8080/oauth/baidu/callback";

    /** 授权成功后跳转前端地址 */
    private String frontendRedirectUri = "http://127.0.0.1:5173/notebook";

    /** 单用户模式默认用户 ID */
    private Long defaultUserId = 1L;

    public String rootPath() {
        String base = "/apps/" + appFolderName;
        String env = normalizedEnvironmentFolder();
        if (StringUtils.hasText(env)) {
            return base + "/" + env;
        }
        return base;
    }

    /** 展示用环境标识：dev / prod */
    public String environmentLabel() {
        return StringUtils.hasText(normalizedEnvironmentFolder()) ? normalizedEnvironmentFolder() : "prod";
    }

    private String normalizedEnvironmentFolder() {
        if (!StringUtils.hasText(environmentFolder)) {
            return "";
        }
        String trimmed = environmentFolder.trim();
        if ("prod".equalsIgnoreCase(trimmed) || "production".equalsIgnoreCase(trimmed)) {
            return "";
        }
        return trimmed.replaceAll("[/\\\\]", "");
    }

    public String notesDir() {
        return rootPath() + "/notes";
    }

    public String trashDir() {
        return rootPath() + "/trash";
    }

    public String imagesDir() {
        return rootPath() + "/images";
    }

    public String ecommerceImagesDir() {
        return rootPath() + "/ecommerce-images";
    }

    public String salesOrderImportsDir() {
        return rootPath() + "/imports/sales-orders";
    }
}
