package com.ai.manager.common.exception;

import com.ai.manager.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 业务逻辑校验失败时抛出此异常，由全局异常处理器统一捕获并返回 ApiResult。
 * 继承自 RuntimeException，无需在方法签名中声明。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final int code;

    /**
     * 通过错误码枚举构建业务异常
     *
     * @param resultCode 错误码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 自定义错误码和消息构建业务异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
