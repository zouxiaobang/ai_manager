package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.NbTodoSaveRequest;
import com.ai.manager.system.domain.vo.NbTodoItemVO;
import com.ai.manager.system.domain.vo.NbTodoMutationVO;

import java.util.List;

/**
 * 待办事项服务接口
 *
 * <p>提供待办事项的列表查询、今日待办、到期提醒、创建、更新、删除和提醒确认等待办管理功能。</p>
 */
public interface NbTodoService {

    /**
     * 查询待办事项列表
     *
     * @param completed   是否已完成
     * @param today     是否今日待办
     * @param pinned    是否置顶
     * @param upcoming  是否即将到来
     * @param unscheduled是否未安排
     * @return 待办事项列表
     */
    List<NbTodoItemVO> list(Boolean completed, Boolean today, Boolean pinned, Boolean upcoming, Boolean unscheduled);

    /**
     * 查询今日待办列表
     *
     * @return 今日待办事项列表
     */
    List<NbTodoItemVO> listToday();

    /**
     * 查询到期提醒列表
     *
     * @return 到期提醒列表
     */
    List<NbTodoItemVO> listDueReminders();

    /**
     * 创建待办事项
     *
     * @param request 待办保存请求参数
     * @return 创建后的待办事项
     */
    NbTodoItemVO create(NbTodoSaveRequest request);

    /**
     * 更新待办事项
     *
     * @param id      待办ID
     * @param request 待办保存请求参数
     * @return 更新后的待办变更结果
     */
    NbTodoMutationVO update(Long id, NbTodoSaveRequest request);

    /**
     * 删除待办事项
     *
     * @param id 待办ID
     */
    void delete(Long id);

    /**
     * 确认提醒
     *
     * @param id 待办ID
     */
    void ackRemind(Long id);
}
