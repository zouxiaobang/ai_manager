package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.vo.BaiduPanAuthStatusVO;
import com.ai.manager.system.service.BaiduPanAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/baidu-pan")
@RequiredArgsConstructor
public class BaiduPanController {

    private final BaiduPanAuthService baiduPanAuthService;

    @GetMapping("/status")
    public ApiResult<BaiduPanAuthStatusVO> status() {
        return ApiResult.ok(baiduPanAuthService.getStatus());
    }
}
