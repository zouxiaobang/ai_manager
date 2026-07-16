package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.DocLibraryFile;
import com.ai.manager.system.domain.vo.DocLibraryStatsVO;
import com.ai.manager.system.domain.vo.DocLibraryTrashItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DocLibraryFileMapper extends BaseMapper<DocLibraryFile> {

    @Update("UPDATE doc_library_file SET deleted = 0, deleted_at = NULL, update_time = NOW() WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

    @Select("""
            select f.id, f.name, f.extension, f.file_size as fileSize, f.deleted_at as deletedAt,
                   f.folder_id as folderId, fol.name as folderName
            from doc_library_file f
            left join doc_library_folder fol on f.folder_id = fol.id
            where f.deleted = 1
            order by f.deleted_at desc
            """)
    List<DocLibraryTrashItemVO> selectTrashList();

    @Select("select count(*) from doc_library_file where deleted = 1")
    long countTrash();

    @Select("select id from doc_library_file where deleted = 1 and id = #{id}")
    Long selectDeletedId(@Param("id") Long id);

    @Delete("DELETE FROM doc_library_file WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    @Update("UPDATE doc_library_file SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE doc_library_file SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(@Param("id") Long id);

    @Update("UPDATE doc_library_file SET kb_status = #{kbStatus}, kb_error = #{kbError}, kb_processed_at = #{kbProcessedAt} WHERE id = #{id}")
    int updateKbStatus(@Param("id") Long id, @Param("kbStatus") String kbStatus,
                       @Param("kbError") String kbError, @Param("kbProcessedAt") LocalDateTime kbProcessedAt);

    @Select("""
            select count(*) as totalFiles, coalesce(sum(file_size), 0) as totalSize
            from doc_library_file where deleted = 0
            """)
    DocLibraryStatsVO selectStats();
}
