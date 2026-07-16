package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.DocLibraryFolder;
import com.ai.manager.system.domain.vo.DocLibraryTreeVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocLibraryFolderMapper extends BaseMapper<DocLibraryFolder> {

    @Select("select id, parent_id as parentId, name, icon, color, sort_order from doc_library_folder where deleted = 0 order by sort_order asc")
    List<DocLibraryTreeVO> selectTreeList();

    @Select("select id from doc_library_file where folder_id = #{folderId} and deleted = 0")
    List<Long> selectFileIdsByFolderId(@Param("folderId") Long folderId);
}
