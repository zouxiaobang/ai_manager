package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.vo.NbNoteTrashItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NbNoteMapper extends BaseMapper<NbNote> {

    @Update("UPDATE nb_note SET deleted = 0, update_time = NOW() WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

    @Select("""
            SELECT n.id, n.title, n.notebook_id AS notebookId, b.name AS notebookName,
                   n.content_excerpt AS contentExcerpt, n.update_time AS updateTime
            FROM nb_note n
            LEFT JOIN nb_notebook b ON n.notebook_id = b.id
            WHERE n.deleted = 1
            ORDER BY n.update_time DESC
            """)
    List<NbNoteTrashItemVO> selectTrashList();

    @Delete("DELETE FROM nb_note WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
