package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新系统用户请求
 * 替代原先的 Map 入参，让昵称等字段获得参数校验。
 */
@Data
public class UpdateUserRequest {

    /** 用户昵称，可选；提供时长度不超过 50 */
    @Size(max = 50, message = "昵称长度不能超过 50 字")
    private String nickname;
}
