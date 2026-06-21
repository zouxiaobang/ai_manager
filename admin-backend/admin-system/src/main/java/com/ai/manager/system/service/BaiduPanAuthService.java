package com.ai.manager.system.service;

import com.ai.manager.system.domain.vo.BaiduPanAuthStatusVO;

public interface BaiduPanAuthService {

    BaiduPanAuthStatusVO getStatus();

    String buildAuthorizeUrl();

    void exchangeCode(String code);

    boolean isAuthorized();

    String requireAccessToken();
}
