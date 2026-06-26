package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.DeploySqlExecuteRequest;
import com.ai.manager.system.domain.vo.DeployDatabaseSnapshotVO;
import com.ai.manager.system.domain.vo.DeploySqlExecuteResultVO;
import com.ai.manager.system.service.DeployDatabaseService;
import com.ai.manager.system.service.DeploySqlTerminalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deploy/database")
public class DeployDatabaseController {

    private final DeployDatabaseService deployDatabaseService;
    private final DeploySqlTerminalService deploySqlTerminalService;

    public DeployDatabaseController(
            DeployDatabaseService deployDatabaseService, DeploySqlTerminalService deploySqlTerminalService) {
        this.deployDatabaseService = deployDatabaseService;
        this.deploySqlTerminalService = deploySqlTerminalService;
    }

    @GetMapping
    public ApiResult<DeployDatabaseSnapshotVO> snapshot() {
        return ApiResult.ok(deployDatabaseService.getSnapshot());
    }

    @PostMapping("/sync")
    public ApiResult<DeployDatabaseSnapshotVO> sync() {
        return ApiResult.ok(deployDatabaseService.sync());
    }

    @PostMapping("/sql/execute")
    public ApiResult<DeploySqlExecuteResultVO> executeSql(@RequestBody DeploySqlExecuteRequest request) {
        String target = request == null ? "local" : request.getTarget();
        String sql = request == null ? "" : request.getSql();
        return ApiResult.ok(deploySqlTerminalService.execute(target, sql));
    }
}
