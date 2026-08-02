package com.ai.manager.common.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiResult 工厂方法与字段断言。
 * 覆盖 ok/fail 各重载，验证 code/message/data/timestamp 语义。
 */
class ApiResultTest {

    @Test
    void ok_withData_shouldSetSuccessFields() {
        ApiResult<String> result = ApiResult.ok("hello");

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("hello");
        assertThat(result.getTimestamp()).isPositive();
    }

    @Test
    void ok_withoutData_shouldSetDataNull() {
        ApiResult<Void> result = ApiResult.ok();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isPositive();
    }

    @Test
    void fail_withResultCode_shouldMapCodeAndMessage() {
        ApiResult<String> result = ApiResult.fail(ResultCode.NOT_FOUND);

        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMessage()).isEqualTo("资源不存在");
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isPositive();
    }

    @Test
    void fail_withCustomCodeAndMessage_shouldSetAsIs() {
        ApiResult<String> result = ApiResult.fail(1001, "库存不足");

        assertThat(result.getCode()).isEqualTo(1001);
        assertThat(result.getMessage()).isEqualTo("库存不足");
        assertThat(result.getTimestamp()).isPositive();
    }

    @Test
    void setters_shouldAllowMutation() {
        ApiResult<String> result = ApiResult.ok();
        result.setCode(500);
        result.setMessage("服务器内部错误");
        result.setData("detail");

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("服务器内部错误");
        assertThat(result.getData()).isEqualTo("detail");
    }
}
