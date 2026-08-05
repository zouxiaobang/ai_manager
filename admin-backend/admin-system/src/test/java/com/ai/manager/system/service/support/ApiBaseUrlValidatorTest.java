package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ApiBaseUrlValidator 单测：公网地址放行、内网/环回/链路本地/元数据地址拒绝、非法 URL 与协议拒绝、可关闭。
 *
 * <p>断言全部使用 IP 字面量（不经外部 DNS），保证离线环境稳定。</p>
 */
class ApiBaseUrlValidatorTest {

    private final ApiBaseUrlValidator validator = new ApiBaseUrlValidator(true);

    @Test
    void validate_公网地址放行() {
        assertThatCode(() -> validator.validate("https://8.8.8.8/v1")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("http://1.1.1.1/v1")).doesNotThrowAnyException();
    }

    @Test
    void validate_null或空白_直接放行() {
        assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("  ")).doesNotThrowAnyException();
    }

    @Test
    void validate_环回地址_拒绝() {
        assertThatThrownBy(() -> validator.validate("http://localhost:8080/v1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("SSRF");
        assertThatThrownBy(() -> validator.validate("http://127.0.0.1:9000/v1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("SSRF");
    }

    @Test
    void validate_内网地址_拒绝() {
        assertThatThrownBy(() -> validator.validate("http://10.0.0.5/v1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("SSRF");
        assertThatThrownBy(() -> validator.validate("http://192.168.1.10/v1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("SSRF");
        assertThatThrownBy(() -> validator.validate("http://172.16.0.1/v1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("SSRF");
        // 云元数据端点
        assertThatThrownBy(() -> validator.validate("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("SSRF");
    }

    @Test
    void validate_非法URL或协议_拒绝() {
        assertThatThrownBy(() -> validator.validate("not-a-url"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator.validate("ftp://8.8.8.8/x"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator.validate("http:///nohost"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_关闭时跳过() {
        ApiBaseUrlValidator disabled = new ApiBaseUrlValidator(false);
        assertThatCode(() -> disabled.validate("http://localhost:8080")).doesNotThrowAnyException();
    }
}
