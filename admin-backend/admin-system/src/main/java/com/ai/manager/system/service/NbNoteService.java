package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.NbNoteSaveRequest;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.vo.NbNoteDetailVO;
import com.ai.manager.system.domain.vo.NbNoteListMetaVO;
import com.ai.manager.system.domain.vo.NbNoteTrashItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 笔记服务接口
 *
 * <p>提供笔记的详情查看、最近笔记列表、搜索、创建、更新、元信息批量查询、删除、
 * 回收站列表、恢复、永久删除和清空回收站等笔记管理功能。</p>
 */
public interface NbNoteService extends IService<NbNote> {

    /**
     * 获取笔记详情
     *
     * @param id 笔记ID
     * @return 笔记详情信息
     */
    NbNoteDetailVO getNoteDetail(Long id);

    /**
     * 查询最近笔记列表
     *
     * @param limit 限制数量
     * @return 最近笔记列表
     */
    List<NbNoteDetailVO> listRecent(int limit);

    /**
     * 搜索笔记
     *
     * @param keyword 关键词
     * @return 搜索结果笔记列表
     */
    List<NbNoteDetailVO> searchNotes(String keyword);

    /**
     * 创建笔记
     *
     * @param request 笔记保存请求参数
     * @return 创建后的笔记详情
     */
    NbNoteDetailVO createNote(NbNoteSaveRequest request);

    /**
     * 更新笔记
     *
     * @param id      笔记ID
     * @param request 笔记保存请求参数
     * @return 更新后的笔记详情
     */
    NbNoteDetailVO updateNote(Long id, NbNoteSaveRequest request);

    /**
     * 根据ID列表批量查询笔记元信息
     *
     * @param ids 笔记ID列表
     * @return 笔记元信息列表
     */
    List<NbNoteListMetaVO> listMetaByIds(List<Long> ids);

    /**
     * 删除笔记（移入回收站）
     *
     * @param id 笔记ID
     */
    void deleteNote(Long id);

    /**
     * 查询回收站笔记列表
     *
     * @return 回收站笔记列表
     */
    List<NbNoteTrashItemVO> listTrash();

    /**
     * 恢复笔记（从回收站恢复）
     *
     * @param id 笔记ID
     */
    void restoreNote(Long id);

    /**
     * 永久删除笔记
     *
     * @param id 笔记ID
     */
    void purgeNote(Long id);

    /**
     * 清空回收站
     */
    void purgeAllTrash();
}
