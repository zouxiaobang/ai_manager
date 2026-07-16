package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.DocLibraryFileTag;
import com.ai.manager.system.domain.entity.DocLibraryTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DocLibraryFileTagMapper extends BaseMapper<DocLibraryFileTag> {

    @Select("select t.* from doc_library_tag t join doc_library_file_tag ft on t.id = ft.tag_id where ft.file_id = #{fileId} and t.deleted = 0")
    List<DocLibraryTag> selectTagsByFileId(@Param("fileId") Long fileId);

    @Delete("DELETE FROM doc_library_file_tag WHERE file_id = #{fileId}")
    int deleteByFileId(@Param("fileId") Long fileId);

    @Select({"<script>",
             "select tag_id, count(*) as cnt from doc_library_file_tag where tag_id in ",
             "<foreach item='tagId' collection='tagIds' open='(' separator=',' close=')'>#{tagId}</foreach>",
             "group by tag_id",
             "</script>"})
    List<Map<String, Object>> selectTagUsageCount(@Param("tagIds") List<Long> tagIds);
}
