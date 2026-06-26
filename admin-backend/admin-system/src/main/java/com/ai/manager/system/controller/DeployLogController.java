package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.DeployLogAnalyzeRequest;
import com.ai.manager.system.domain.vo.DeployLogAiAnalyzeVO;
import com.ai.manager.system.domain.vo.DeployLogStatsVO;
import com.ai.manager.system.domain.vo.DeployLogTailVO;
import com.ai.manager.system.service.DeployLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/deploy/logs")
public class DeployLogController {

    private final DeployLogService deployLogService;

    public DeployLogController(DeployLogService deployLogService) {
        this.deployLogService = deployLogService;
    }

    @GetMapping("/tail")
    public ApiResult<DeployLogTailVO> tail(
            @RequestParam(value = "lines", defaultValue = "100") int lines,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResult.ok(deployLogService.tail(lines, level, keyword));
    }

    @GetMapping("/stats")
    public ApiResult<DeployLogStatsVO> stats(@RequestParam(value = "hours", defaultValue = "24") int hours) {
        return ApiResult.ok(deployLogService.stats(hours));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return deployLogService.stream(level, keyword);
    }

    @PostMapping("/ai-analyze")
    public ApiResult<DeployLogAiAnalyzeVO> analyze(@RequestBody(required = false) DeployLogAnalyzeRequest request) {
        return ApiResult.ok(deployLogService.analyze(request));
    }
}
