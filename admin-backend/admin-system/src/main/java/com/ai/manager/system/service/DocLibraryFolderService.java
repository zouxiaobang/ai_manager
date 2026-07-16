package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.DocLibraryFolderSaveRequest;
import com.ai.manager.system.domain.entity.DocLibraryFolder;
import com.ai.manager.system.domain.vo.DocLibraryTreeVO;

import java.util.List;

public interface DocLibraryFolderService {

    List<DocLibraryTreeVO> getTree();

    DocLibraryTreeVO createFolder(DocLibraryFolderSaveRequest req);

    DocLibraryTreeVO updateFolder(Long id, DocLibraryFolderSaveRequest req);

    void deleteFolder(Long id);

    DocLibraryFolder getById(Long id);
}
