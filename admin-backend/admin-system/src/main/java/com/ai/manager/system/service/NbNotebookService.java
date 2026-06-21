package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.NbNotebookSaveRequest;
import com.ai.manager.system.domain.entity.NbNotebook;
import com.ai.manager.system.domain.vo.NbTreeNodeVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NbNotebookService extends IService<NbNotebook> {

    List<NbTreeNodeVO> buildTree();

    NbNotebook createNotebook(NbNotebookSaveRequest request);

    NbNotebook updateNotebook(Long id, NbNotebookSaveRequest request);

    void deleteNotebook(Long id);
}
