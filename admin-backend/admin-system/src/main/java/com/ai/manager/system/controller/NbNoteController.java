package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.NbNoteMetaBatchRequest;
import com.ai.manager.system.domain.dto.NbNoteSaveRequest;
import com.ai.manager.system.domain.vo.NbNoteDetailVO;
import com.ai.manager.system.domain.vo.NbNoteListMetaVO;
import com.ai.manager.system.domain.vo.NbNoteTrashItemVO;
import com.ai.manager.system.service.NbNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记控制器
 *
 * <p>所属模块：笔记模块-笔记管理</p>
 * <p>API路径前缀：/api/notes</p>
 * <p>功能描述：提供笔记的增删改查、搜索、回收站、恢复等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NbNoteController {

    private final NbNoteService nbNoteService;

    /**
     * 搜索笔记
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/notes/search</p>
     *
     * @param keyword 搜索关键词
     * @return 笔记列表
     */
    @GetMapping("/search")
    public ApiResult<List<NbNoteDetailVO>> search(@RequestParam String keyword) {
        return ApiResult.ok(nbNoteService.searchNotes(keyword));
    }

    /**
     * 获取最近笔记列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/notes/recent</p>
     *
     * @param limit 返回数量限制
     * @return 最近笔记列表
     */
    @GetMapping("/recent")
    public ApiResult<List<NbNoteDetailVO>> recent(@RequestParam(required = false, defaultValue = "20") int limit) {
        return ApiResult.ok(nbNoteService.listRecent(limit));
    }

    /**
     * 批量获取笔记元数据
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/notes/meta</p>
     *
     * @param request 笔记ID批量请求参数
     * @return 笔记元数据列表
     */
    @PostMapping("/meta")
    public ApiResult<List<NbNoteListMetaVO>> meta(@jakarta.validation.Valid @RequestBody NbNoteMetaBatchRequest request) {
        List<Long> ids = request == null ? List.of() : request.getIds();
        return ApiResult.ok(nbNoteService.listMetaByIds(ids));
    }

    /**
     * 获取回收站笔记列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/notes/trash</p>
     *
     * @return 回收站笔记列表
     */
    @GetMapping("/trash")
    public ApiResult<List<NbNoteTrashItemVO>> trash() {
        return ApiResult.ok(nbNoteService.listTrash());
    }

    /**
     * 获取笔记详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/notes/{id}</p>
     *
     * @param id 笔记ID
     * @return 笔记详情
     */
    @GetMapping("/{id}")
    public ApiResult<NbNoteDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(nbNoteService.getNoteDetail(id));
    }

    /**
     * 创建笔记
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/notes</p>
     *
     * @param request 笔记保存请求参数
     * @return 创建后的笔记详情
     */
    @PostMapping
    public ApiResult<NbNoteDetailVO> create(@jakarta.validation.Valid @RequestBody NbNoteSaveRequest request) {
        return ApiResult.ok(nbNoteService.createNote(request));
    }

    /**
     * 更新笔记
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/notes/{id}</p>
     *
     * @param id 笔记ID
     * @param request 笔记保存请求参数
     * @return 更新后的笔记详情
     */
    @PutMapping("/{id}")
    public ApiResult<NbNoteDetailVO> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody NbNoteSaveRequest request) {
        return ApiResult.ok(nbNoteService.updateNote(id, request));
    }

    /**
     * 删除笔记（移入回收站）
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/notes/{id}</p>
     *
     * @param id 笔记ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        nbNoteService.deleteNote(id);
        return ApiResult.ok();
    }

    /**
     * 恢复笔记
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/notes/{id}/restore</p>
     *
     * @param id 笔记ID
     * @return 操作结果
     */
    @PostMapping("/{id}/restore")
    public ApiResult<Void> restore(@PathVariable Long id) {
        nbNoteService.restoreNote(id);
        return ApiResult.ok();
    }

    /**
     * 永久删除笔记
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/notes/{id}/purge</p>
     *
     * @param id 笔记ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}/purge")
    public ApiResult<Void> purge(@PathVariable Long id) {
        nbNoteService.purgeNote(id);
        return ApiResult.ok();
    }

    /**
     * 清空回收站
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/notes/trash</p>
     *
     * @return 操作结果
     */
    @DeleteMapping("/trash")
    public ApiResult<Void> purgeAll() {
        nbNoteService.purgeAllTrash();
        return ApiResult.ok();
    }
}
