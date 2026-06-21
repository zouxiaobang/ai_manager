package com.ai.manager.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

    /** OAuth 回调地址，需在百度开放平台配置一致 */
    private String redirectUri = "http://127.0.0.1:8080/oauth/baidu/callback";

    /** 授权成功后跳转前端地址 */
    private String frontendRedirectUri = "http://127.0.0.1:5173/notebook";

    /** 单用户模式默认用户 ID */
    private Long defaultUserId = 1L;

    public String rootPath() {
        return "/apps/" + appFolderName;
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
}
