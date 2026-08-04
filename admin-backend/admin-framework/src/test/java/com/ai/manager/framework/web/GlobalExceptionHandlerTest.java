package com.ai.manager.framework.web;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 单元测试
 * 直接构造各类异常调用 @ExceptionHandler 方法，断言返回的 ApiResult 状态码与消息。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusiness_shouldReturnExceptionCodeAndMessage() {
        BusinessException ex = new BusinessException(5001, "库存不足");

        ApiResult<Void> result = handler.handleBusiness(ex);

        assertThat(result.getCode()).isEqualTo(5001);
        assertThat(result.getMessage()).isEqualTo("库存不足");
    }

    @Test
    void handleValidation_withMethodArgumentNotValidShouldReturnFieldMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        FieldError fe = mock(FieldError.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldError()).thenReturn(fe);
        when(fe.getDefaultMessage()).thenReturn("名称必填");

        ApiResult<Void> result = handler.handleValidation(ex);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
        assertThat(result.getMessage()).isEqualTo("名称必填");
    }

    @Test
    void handleValidation_withoutFieldErrorShouldFallbackToBadRequestMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldError()).thenReturn(null);

        ApiResult<Void> result = handler.handleValidation(ex);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.BAD_REQUEST.getMessage());
    }

    @Test
    void handleValidation_withBindExceptionShouldReturnFieldMessage() {
        BindException bind = new BindException("target", "obj");
        bind.addError(new FieldError("obj", "name", "姓名必填"));

        ApiResult<Void> result = handler.handleValidation(bind);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
        assertThat(result.getMessage()).isEqualTo("姓名必填");
    }

    @Test
    void handleIo_brokenPipe_shouldReturnInternalError() {
        ApiResult<Void> result = handler.handleIo(new IOException("Broken pipe"));

        assertThat(result.getCode()).isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
    }

    @Test
    void handleIo_otherError_shouldReturnInternalError() {
        ApiResult<Void> result = handler.handleIo(new IOException("disk full"));

        assertThat(result.getCode()).isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
    }

    @Test
    void handleOther_shouldReturnInternalError() {
        ApiResult<Void> result = handler.handleOther(new Exception("boom"));

        assertThat(result.getCode()).isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
    }
}
