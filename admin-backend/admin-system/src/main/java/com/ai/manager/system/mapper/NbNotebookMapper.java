package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.NbNotebook;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NbNotebookMapper extends BaseMapper<NbNotebook> {

    @Update("UPDATE nb_notebook SET deleted = 0, update_time = NOW() WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
