package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class BaiduPanAuthStatusVO {

    private boolean authorized;

    private String authorizeUrl;

    private Long baiduUid;

    private String expiresAt;
}
