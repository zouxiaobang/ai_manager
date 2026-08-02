package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.NbNotebookSaveRequest;
import com.ai.manager.system.domain.vo.NbNotebookVO;
import com.ai.manager.system.domain.vo.NbTreeNodeVO;
import com.ai.manager.system.service.NbNotebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记本控制器
 *
 * <p>所属模块：笔记模块-笔记本管理</p>
 * <p>API路径前缀：/api/notebooks</p>
 * <p>功能描述：提供笔记本的增删改查、笔记本树结构等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/notebooks")
@RequiredArgsConstructor
public class NbNotebookController {

    private final NbNotebookService nbNotebookService;

    /**
     * 获取笔记本树结构
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/notebooks/tree</p>
     *
     * @return 笔记本树节点列表
     */
    @GetMapping("/tree")
    public ApiResult<List<NbTreeNodeVO>> tree() {
        return ApiResult.ok(nbNotebookService.buildTree());
    }

    /**
     * 创建笔记本
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/notebooks</p>
     *
     * @param request 笔记本保存请求参数
     * @return 创建后的笔记本信息
     */
    @PostMapping
    public ApiResult<NbNotebookVO> create(@jakarta.validation.Valid @RequestBody NbNotebookSaveRequest request) {
        return ApiResult.ok(nbNotebookService.createNotebook(request));
    }

    /**
     * 更新笔记本
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/notebooks/{id}</p>
     *
     * @param id 笔记本ID
     * @param request 笔记本保存请求参数
     * @return 更新后的笔记本信息
     */
    @PutMapping("/{id}")
    public ApiResult<NbNotebookVO> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody NbNotebookSaveRequest request) {
        return ApiResult.ok(nbNotebookService.updateNotebook(id, request));
    }

    /**
     * 删除笔记本
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/notebooks/{id}</p>
     *
     * @param id 笔记本ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        nbNotebookService.deleteNotebook(id);
        return ApiResult.ok();
    }
}
