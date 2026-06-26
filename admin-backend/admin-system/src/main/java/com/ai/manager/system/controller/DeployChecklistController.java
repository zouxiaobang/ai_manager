package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.service.DeployChecklistService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/deploy/checklist")
public class DeployChecklistController {

    private final DeployChecklistService deployChecklistService;

    public DeployChecklistController(DeployChecklistService deployChecklistService) {
        this.deployChecklistService = deployChecklistService;
    }

    @GetMapping("/logs")
    public ApiResult<Map<String, Object>> logs() {
        return ApiResult.ok(deployChecklistService.checkLogs());
    }
}
