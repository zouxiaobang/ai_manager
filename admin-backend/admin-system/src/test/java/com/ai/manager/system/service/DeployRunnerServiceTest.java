package com.ai.manager.system.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 部署运行器服务单元测试
 */
class DeployRunnerServiceTest {

    @Test
    void resolveConsoleCharset_始终UTF8避免部署日志中文乱码() {
        // 部署脚本设置 [Console]::OutputEncoding=UTF8、Maven/npm/ssh 输出亦为 UTF-8；
        // 若按 Windows 系统 GBK 读进程输出，中文日志会被解码成乱码。
        assertThat(DeployRunnerService.resolveConsoleCharset()).isEqualTo(StandardCharsets.UTF_8);
    }
}
