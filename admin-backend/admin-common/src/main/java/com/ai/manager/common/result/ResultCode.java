package com.ai.manager.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API响应状态码枚举
 * <p>
 * 定义所有接口可能返回的错误码及其描述信息。
 * 成功固定为 0，失败码参考 HTTP 状态码设计。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(0, "success"),

    /** 请求参数错误 - 400 */
    BAD_REQUEST(400, "请求参数错误"),

    /** 未授权 - 401 */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /** 禁止访问 - 403 */
    FORBIDDEN(403, "无权限访问"),

    /** 资源不存在 - 404 */
    NOT_FOUND(404, "资源不存在"),

    /** 资源冲突 - 409 */
    CONFLICT(409, "资源冲突"),

    /** 服务器内部错误 - 500 */
    INTERNAL_ERROR(500, "服务器内部错误");

    /** 错误码 */
    private final int code;

    /** 错误消息 */
    private final String message;
}
