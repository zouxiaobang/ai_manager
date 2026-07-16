package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.DocLibraryFileBatchMoveRequest;
import com.ai.manager.system.domain.dto.DocLibraryFileMoveRequest;
import com.ai.manager.system.domain.dto.DocLibraryFileRenameRequest;
import com.ai.manager.system.domain.dto.DocLibrarySearchRequest;
import com.ai.manager.system.domain.vo.DocLibraryFileDetailVO;
import com.ai.manager.system.domain.vo.DocLibraryFileVO;
import com.ai.manager.system.domain.vo.DocLibraryStatsVO;
import com.ai.manager.system.domain.vo.DocLibraryTrashItemVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface DocLibraryFileService {

    IPage<DocLibraryFileVO> listFiles(Long folderId, String sort, String order, int page, int size);

    DocLibraryFileDetailVO getFileDetail(Long id);

    void renameFile(Long id, DocLibraryFileRenameRequest req);

    void moveFile(Long id, DocLibraryFileMoveRequest req);

    void batchMove(Long folderId, DocLibraryFileBatchMoveRequest req);

    void togglePin(Long id);

    void updateDescription(Long id, String description);

    void softDelete(Long id);

    void batchDelete(List<Long> ids);

    List<DocLibraryTrashItemVO> listTrash();

    void restoreFile(Long id);

    void purgeFile(Long id);

    void purgeAllTrash();

    void incrementView(Long id);

    void incrementDownload(Long id);

    String toggleKbStatus(Long id);

    IPage<DocLibraryFileVO> listKbReadyFiles(int page, int size);

    DocLibraryStatsVO getKbStats();

    IPage<DocLibraryFileVO> search(DocLibrarySearchRequest req);

    IPage<DocLibraryFileVO> listFavorites(int page, int size);

    List<DocLibraryFileVO> listRecent(int limit);

    DocLibraryStatsVO getStats();

    DocLibraryStatsVO getFolderStats(Long folderId);
}
