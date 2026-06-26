package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.service.DeployRunnerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/deploy")
public class DeployRunController {

    private final DeployRunnerService deployRunnerService;

    public DeployRunController(DeployRunnerService deployRunnerService) {
        this.deployRunnerService = deployRunnerService;
    }

    @GetMapping("/runner/status")
    public ApiResult<Map<String, Object>> runnerStatus() {
        return ApiResult.ok(deployRunnerService.status());
    }

    @GetMapping("/runner/preflight")
    public ApiResult<Map<String, Object>> runnerPreflight() {
        return ApiResult.ok(deployRunnerService.preflight());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("target") String target) {
        return deployRunnerService.startStream(target);
    }
}
