package com.ai.manager.framework.web;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusiness(BusinessException ex) {
        return ApiResult.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResult<Void> handleValidation(Exception ex) {
        String message = ResultCode.BAD_REQUEST.getMessage();
        if (ex instanceof MethodArgumentNotValidException manv) {
            if (manv.getBindingResult().getFieldError() != null) {
                message = manv.getBindingResult().getFieldError().getDefaultMessage();
            }
        } else if (ex instanceof BindException bind) {
            if (bind.getBindingResult().getFieldError() != null) {
                message = bind.getBindingResult().getFieldError().getDefaultMessage();
            }
        }
        return ApiResult.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResult.fail(ResultCode.INTERNAL_ERROR);
    }
}
