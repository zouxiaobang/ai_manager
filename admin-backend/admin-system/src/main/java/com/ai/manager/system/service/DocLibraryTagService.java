package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.DocLibraryTagSaveRequest;
import com.ai.manager.system.domain.vo.DocLibraryTagVO;

import java.util.List;

public interface DocLibraryTagService {

    List<DocLibraryTagVO> listAll();

    DocLibraryTagVO createTag(DocLibraryTagSaveRequest req);

    DocLibraryTagVO updateTag(Long id, DocLibraryTagSaveRequest req);

    void deleteTag(Long id);

    void syncFileTags(Long fileId, List<Long> tagIds);

    List<DocLibraryTagVO> getTagsByFileId(Long fileId);
}
