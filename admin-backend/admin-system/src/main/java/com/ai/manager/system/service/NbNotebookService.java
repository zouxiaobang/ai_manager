package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.NbNotebookSaveRequest;
import com.ai.manager.system.domain.entity.NbNotebook;
import com.ai.manager.system.domain.vo.NbTreeNodeVO;
import com.ai.manager.system.domain.vo.NbNotebookVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 笔记本服务接口
 *
 * <p>提供笔记本的树形结构构建、创建、更新和删除等笔记本管理功能。</p>
 */
public interface NbNotebookService extends IService<NbNotebook> {

    /**
     * 构建笔记本树形结构
     *
     * @return 笔记本树节点列表
     */
    List<NbTreeNodeVO> buildTree();

    /**
     * 创建笔记本
     *
     * @param request 笔记本保存请求参数
     * @return 创建后的笔记本信息
     */
    NbNotebookVO createNotebook(NbNotebookSaveRequest request);

    /**
     * 更新笔记本
     *
     * @param id      笔记本ID
     * @param request 笔记本保存请求参数
     * @return 更新后的笔记本信息
     */
    NbNotebookVO updateNotebook(Long id, NbNotebookSaveRequest request);

    /**
     * 删除笔记本
     *
     * @param id 笔记本ID
     */
    void deleteNotebook(Long id);
}
