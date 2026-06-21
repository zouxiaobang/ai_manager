package com.ai.manager.system.controller;

import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.service.BaiduPanAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 百度 OAuth 回调（路径需与开放平台 redirect_uri 完全一致）。
 */
@RestController
@RequiredArgsConstructor
public class BaiduPanOAuthCallbackController {

    private final BaiduPanAuthService baiduPanAuthService;
    private final BaiduPanProperties baiduPanProperties;

    @GetMapping("/oauth/baidu/callback")
    public void oauthCallback(@RequestParam(required = false) String code,
                              @RequestParam(required = false) String error,
                              HttpServletResponse response) throws IOException {
        if (StringUtils.hasText(error)) {
            redirect(response, "error=" + url(error));
            return;
        }
        baiduPanAuthService.exchangeCode(code);
        redirect(response, "baidu=connected");
    }

    private void redirect(HttpServletResponse response, String query) throws IOException {
        String base = baiduPanProperties.getFrontendRedirectUri();
        String target = base + (base.contains("?") ? "&" : "?") + query;
        response.sendRedirect(target);
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
