package com.ai.manager.common.result;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResultCode 枚举语义断言：成功固定 0、错误码唯一、消息非空。
 */
class ResultCodeTest {

    @Test
    void success_shouldBeZero() {
        assertThat(ResultCode.SUCCESS.getCode()).isZero();
        assertThat(ResultCode.SUCCESS.getMessage()).isEqualTo("success");
    }

    @Test
    void allCodes_shouldBeUnique() {
        Set<Integer> codes = new HashSet<>();
        for (ResultCode code : ResultCode.values()) {
            assertThat(codes.add(code.getCode())).as("错误码 %s 重复", code).isTrue();
        }
    }

    @Test
    void allMessages_shouldBeNonBlank() {
        for (ResultCode code : ResultCode.values()) {
            assertThat(code.getMessage()).isNotBlank();
        }
    }

    @Test
    void errorCodes_shouldFollowHttpStyle() {
        assertThat(ResultCode.BAD_REQUEST.getCode()).isEqualTo(400);
        assertThat(ResultCode.UNAUTHORIZED.getCode()).isEqualTo(401);
        assertThat(ResultCode.FORBIDDEN.getCode()).isEqualTo(403);
        assertThat(ResultCode.NOT_FOUND.getCode()).isEqualTo(404);
        assertThat(ResultCode.CONFLICT.getCode()).isEqualTo(409);
        assertThat(ResultCode.INTERNAL_ERROR.getCode()).isEqualTo(500);
    }
}
