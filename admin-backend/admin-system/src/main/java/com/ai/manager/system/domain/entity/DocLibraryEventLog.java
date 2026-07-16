package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("doc_library_event_log")
public class DocLibraryEventLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String event;

    private Long fileId;

    private Long folderId;

    private String paramsJson;

    private LocalDateTime createTime;
}
