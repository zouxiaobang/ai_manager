package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户响应 VO
 * 对外暴露用户信息，剔除逻辑删除标记 deleted 等内部字段，避免实体直接泄露到前端。
 */
@Data
public class SysUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
