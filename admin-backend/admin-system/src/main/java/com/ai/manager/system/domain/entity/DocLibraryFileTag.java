package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("doc_library_file_tag")
public class DocLibraryFileTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private Long tagId;
}
