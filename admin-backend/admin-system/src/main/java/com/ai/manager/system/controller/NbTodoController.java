package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.NbTodoSaveRequest;
import com.ai.manager.system.domain.vo.NbTodoItemVO;
import com.ai.manager.system.domain.vo.NbTodoMutationVO;
import com.ai.manager.system.service.NbTodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 待办事项控制器
 *
 * <p>所属模块：笔记模块-待办管理</p>
 * <p>API路径前缀：/api/todos</p>
 * <p>功能描述：提供待办事项的增删改查、今日待办、提醒等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class NbTodoController {

    private final NbTodoService nbTodoService;

    /**
     * 查询待办事项列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/todos</p>
     *
     * @param completed 是否已完成
     * @param today 是否今日待办
     * @param pinned 是否置顶
     * @param upcoming 是否即将到来
     * @param unscheduled 是否未安排
     * @return 待办事项列表
     */
    @GetMapping
    public ApiResult<List<NbTodoItemVO>> list(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean today,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) Boolean upcoming,
            @RequestParam(required = false) Boolean unscheduled) {
        return ApiResult.ok(nbTodoService.list(completed, today, pinned, upcoming, unscheduled));
    }

    /**
     * 获取今日待办列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/todos/today</p>
     *
     * @return 今日待办列表
     */
    @GetMapping("/today")
    public ApiResult<List<NbTodoItemVO>> today() {
        return ApiResult.ok(nbTodoService.listToday());
    }

    /**
     * 获取到期提醒列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/todos/reminders/due</p>
     *
     * @return 到期提醒列表
     */
    @GetMapping("/reminders/due")
    public ApiResult<List<NbTodoItemVO>> dueReminders() {
        return ApiResult.ok(nbTodoService.listDueReminders());
    }

    /**
     * 确认提醒
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/todos/{id}/remind-ack</p>
     *
     * @param id 待办事项ID
     * @return 操作结果
     */
    @PostMapping("/{id}/remind-ack")
    public ApiResult<Void> ackRemind(@PathVariable Long id) {
        nbTodoService.ackRemind(id);
        return ApiResult.ok();
    }

    /**
     * 创建待办事项
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/todos</p>
     *
     * @param request 待办事项保存请求参数
     * @return 创建后的待办事项
     */
    @PostMapping
    public ApiResult<NbTodoItemVO> create(@jakarta.validation.Valid @RequestBody NbTodoSaveRequest request) {
        return ApiResult.ok(nbTodoService.create(request));
    }

    /**
     * 更新待办事项
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/todos/{id}</p>
     *
     * @param id 待办事项ID
     * @param request 待办事项保存请求参数
     * @return 更新后的待办事项
     */
    @PutMapping("/{id}")
    public ApiResult<NbTodoMutationVO> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody NbTodoSaveRequest request) {
        return ApiResult.ok(nbTodoService.update(id, request));
    }

    /**
     * 删除待办事项
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/todos/{id}</p>
     *
     * @param id 待办事项ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        nbTodoService.delete(id);
        return ApiResult.ok();
    }
}
