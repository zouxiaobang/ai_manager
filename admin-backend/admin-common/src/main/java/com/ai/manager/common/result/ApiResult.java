package com.ai.manager.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局统一API响应结果封装类
 * <p>
 * 所有Controller接口的返回值都应使用此类进行包装，
 * 确保前后端交互的响应格式统一。
 * </p>
 *
 * @param <T> 响应数据的泛型类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     * 0 表示成功，非0表示失败
     * 详见 {@link ResultCode}
     */
    private int code;

    /**
     * 响应消息
     * 成功时通常为 "success"，失败时为错误描述
     */
    private String message;

    /**
     * 响应数据
     * 为 null 时 JSON 序列化时自动忽略
     */
    private T data;

    /**
     * 响应时间戳（毫秒）
     * 服务端生成响应的时间
     */
    private long timestamp;

    /**
     * 构建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功的ApiResult对象
     */
    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 构建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功的ApiResult对象（data为null）
     */
    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    /**
     * 构建失败响应（通过错误码枚举）
     *
     * @param resultCode 错误码枚举
     * @param <T>        数据类型
     * @return 失败的ApiResult对象
     */
    public static <T> ApiResult<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 构建失败响应（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败的ApiResult对象
     */
    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
