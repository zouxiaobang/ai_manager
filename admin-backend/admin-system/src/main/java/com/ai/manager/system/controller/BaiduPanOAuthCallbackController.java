package com.ai.manager.system.controller;

import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.NoteContentSyncService;
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
    private final NoteContentSyncService noteContentSyncService;

    @GetMapping("/oauth/baidu/callback")
    public void oauthCallback(@RequestParam(required = false) String code,
                              @RequestParam(required = false) String error,
                              @RequestParam(required = false) String state,
                              HttpServletResponse response) throws IOException {
        if (StringUtils.hasText(error)) {
            redirect(response, state, "baidu=error&error=" + url(error));
            return;
        }
        baiduPanAuthService.exchangeCode(code);
        noteContentSyncService.scheduleReconcileAll();
        redirect(response, state, "baidu=connected");
    }

    private void redirect(HttpServletResponse response, String state, String query) throws IOException {
        response.sendRedirect(resolveFrontendRedirect(state, query));
    }

    private String resolveFrontendRedirect(String state, String query) {
        String configured = baiduPanProperties.getFrontendRedirectUri();
        int hashPos = configured.indexOf("/#");
        String siteOrigin = hashPos >= 0 ? configured.substring(0, hashPos) : configured;
        String hashPath = "/home";
        if (StringUtils.hasText(state)) {
            hashPath = state.startsWith("/") ? state : "/" + state;
        } else if (hashPos >= 0) {
            hashPath = configured.substring(hashPos + 1);
            int queryPos = hashPath.indexOf('?');
            if (queryPos >= 0) {
                hashPath = hashPath.substring(0, queryPos);
            }
        }
        if (!hashPath.startsWith("/")) {
            hashPath = "/" + hashPath;
        }
        return siteOrigin + "/#" + hashPath + "?" + query;
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
