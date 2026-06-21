package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("nb_note_tag_rel")
public class NbNoteTagRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long tagId;
}
