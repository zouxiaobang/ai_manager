package com.ai.manager.framework.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FaviconController 单元测试
 * 验证 /favicon.ico 返回 204，避免浏览器请求触发 NoResourceFoundException。
 */
class FaviconControllerTest {

    private final FaviconController controller = new FaviconController();

    @Test
    void favicon_shouldReturnNoContent() {
        ResponseEntity<Void> result = controller.favicon();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
