package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.client.BaiduPanClient;
import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.domain.entity.NbBaiduPanAuth;
import com.ai.manager.system.domain.vo.BaiduPanAuthStatusVO;
import com.ai.manager.system.mapper.NbBaiduPanAuthMapper;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaiduPanAuthServiceImpl implements BaiduPanAuthService {

    private final NbBaiduPanAuthMapper nbBaiduPanAuthMapper;
    private final BaiduPanClient baiduPanClient;
    private final BaiduPanProperties baiduPanProperties;

    @Override
    public BaiduPanAuthStatusVO getStatus() {
        BaiduPanAuthStatusVO vo = new BaiduPanAuthStatusVO();
        NbBaiduPanAuth auth = findAuth();
        vo.setAuthorized(auth != null);
        vo.setAuthorizeUrl(buildAuthorizeUrl());
        if (auth != null) {
            vo.setBaiduUid(auth.getBaiduUid());
            vo.setExpiresAt(auth.getExpiresAt() == null ? null : auth.getExpiresAt().toString());
        }
        return vo;
    }

    @Override
    public String buildAuthorizeUrl() {
        if (!StringUtils.hasText(baiduPanProperties.getAppKey())) {
            return "";
        }
        String redirect = url(baiduPanProperties.getRedirectUri());
        return "https://openapi.baidu.com/oauth/2.0/authorize?response_type=code&client_id="
                + url(baiduPanProperties.getAppKey())
                + "&redirect_uri=" + redirect
                + "&scope=basic,netdisk&display=popup";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exchangeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "授权码不能为空");
        }
        if (!StringUtils.hasText(baiduPanProperties.getAppKey())
                || !StringUtils.hasText(baiduPanProperties.getSecretKey())) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "未配置百度网盘 AppKey/SecretKey");
        }
        try {
            BaiduPanClient.BaiduTokenResponse token = baiduPanClient.exchangeCode(
                    baiduPanProperties.getAppKey(),
                    baiduPanProperties.getSecretKey(),
                    baiduPanProperties.getRedirectUri(),
                    code
            );
            saveToken(token);
        } catch (Exception e) {
            log.error("百度网盘 OAuth 换 token 失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "百度网盘授权失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isAuthorized() {
        try {
            requireAccessToken();
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    @Override
    public String requireAccessToken() {
        NbBaiduPanAuth auth = findAuth();
        if (auth == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先绑定百度网盘");
        }
        if (auth.getExpiresAt() != null && auth.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(2))) {
            return auth.getAccessToken();
        }
        return refreshAccessToken(auth);
    }

    private String refreshAccessToken(NbBaiduPanAuth auth) {
        if (!StringUtils.hasText(baiduPanProperties.getSecretKey())) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "未配置百度网盘 SecretKey");
        }
        try {
            BaiduPanClient.BaiduTokenResponse token = baiduPanClient.refreshToken(
                    baiduPanProperties.getAppKey(),
                    baiduPanProperties.getSecretKey(),
                    auth.getRefreshToken()
            );
            auth.setAccessToken(token.getAccessToken());
            if (StringUtils.hasText(token.getRefreshToken())) {
                auth.setRefreshToken(token.getRefreshToken());
            }
            auth.setExpiresAt(LocalDateTime.now().plusSeconds(Math.max(token.getExpiresIn(), 60)));
            nbBaiduPanAuthMapper.updateById(auth);
            return auth.getAccessToken();
        } catch (Exception e) {
            log.error("刷新百度网盘 token 失败", e);
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "百度网盘授权已失效，请重新绑定");
        }
    }

    private void saveToken(BaiduPanClient.BaiduTokenResponse token) {
        NbBaiduPanAuth auth = findAuth();
        if (auth == null) {
            auth = new NbBaiduPanAuth();
            auth.setUserId(baiduPanProperties.getDefaultUserId());
        }
        auth.setAccessToken(token.getAccessToken());
        auth.setRefreshToken(token.getRefreshToken());
        auth.setExpiresAt(LocalDateTime.now().plusSeconds(Math.max(token.getExpiresIn(), 60)));
        if (auth.getId() == null) {
            nbBaiduPanAuthMapper.insert(auth);
        } else {
            nbBaiduPanAuthMapper.updateById(auth);
        }
    }

    private NbBaiduPanAuth findAuth() {
        return nbBaiduPanAuthMapper.selectOne(new LambdaQueryWrapper<NbBaiduPanAuth>()
                .eq(NbBaiduPanAuth::getUserId, baiduPanProperties.getDefaultUserId())
                .last("LIMIT 1"));
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
