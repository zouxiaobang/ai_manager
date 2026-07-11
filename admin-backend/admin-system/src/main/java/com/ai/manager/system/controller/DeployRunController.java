package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.service.DeployHistoryService;
import com.ai.manager.system.service.DeployRunnerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ai.manager.system.domain.vo.DeployVersionVO;
import java.util.List;
import java.util.Map;

/**
 * 部署运行控制器
 *
 * <p>所属模块：部署模块</p>
 * <p>API路径前缀：/api/deploy</p>
 * <p>功能描述：提供部署运行状态查询、部署预检、部署日志流、部署版本列表等部署管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/deploy")
public class DeployRunController {

    private final DeployRunnerService deployRunnerService;
    private final DeployHistoryService deployHistoryService;

    public DeployRunController(
            DeployRunnerService deployRunnerService, DeployHistoryService deployHistoryService) {
        this.deployRunnerService = deployRunnerService;
        this.deployHistoryService = deployHistoryService;
    }

    /**
     * 获取部署运行器状态
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/deploy/runner/status</p>
     *
     * @return 运行器状态信息
     */
    @GetMapping("/runner/status")
    public ApiResult<Map<String, Object>> runnerStatus() {
        return ApiResult.ok(deployRunnerService.status());
    }

    /**
     * 部署预检
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/deploy/runner/preflight</p>
     *
     * @return 预检结果
     */
    @GetMapping("/runner/preflight")
    public ApiResult<Map<String, Object>> runnerPreflight() {
        return ApiResult.ok(deployRunnerService.preflight());
    }

    /**
     * 获取部署日志流
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/deploy/stream</p>
     *
     * @param target 部署目标
     * @return SSE事件流
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("target") String target) {
        return deployRunnerService.startStream(target);
    }

    /**
     * 获取部署版本列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/deploy/versions</p>
     *
     * @param limit 返回数量限制
     * @return 部署版本列表
     */
    @GetMapping("/versions")
    public ApiResult<List<DeployVersionVO>> versions(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ApiResult.ok(deployHistoryService.list(limit));
    }
}
