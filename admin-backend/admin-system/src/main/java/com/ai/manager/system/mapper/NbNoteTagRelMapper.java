package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.NbNoteTagRel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NbNoteTagRelMapper extends BaseMapper<NbNoteTagRel> {

    @Delete("DELETE FROM nb_note_tag_rel WHERE note_id = #{noteId}")
    int deleteByNoteId(@Param("noteId") Long noteId);
}
