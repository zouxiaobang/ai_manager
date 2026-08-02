package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记本响应 VO
 * 对外暴露笔记本信息，剔除逻辑删除标记 deleted 等内部字段，避免实体直接泄露到前端。
 */
@Data
public class NbNotebookVO {

    private Long id;

    private Long parentId;

    private String name;

    private String icon;

    private String color;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
