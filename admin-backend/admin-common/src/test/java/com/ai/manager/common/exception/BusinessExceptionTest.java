package com.ai.manager.common.exception;

import com.ai.manager.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BusinessException 两种构造方式的 code/message 断言。
 */
class BusinessExceptionTest {

    @Test
    void withResultCode_shouldMapCodeAndMessage() {
        BusinessException ex = new BusinessException(ResultCode.NOT_FOUND);

        assertThat(ex.getCode()).isEqualTo(404);
        assertThat(ex.getMessage()).isEqualTo("资源不存在");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void withCustomCodeAndMessage_shouldSetAsIs() {
        BusinessException ex = new BusinessException(1002, "订单已关闭");

        assertThat(ex.getCode()).isEqualTo(1002);
        assertThat(ex.getMessage()).isEqualTo("订单已关闭");
    }
}
