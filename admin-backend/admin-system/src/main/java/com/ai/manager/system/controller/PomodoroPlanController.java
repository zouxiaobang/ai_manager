package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.PomodoroPlanSaveRequest;
import com.ai.manager.system.domain.entity.PomodoroPlan;
import com.ai.manager.system.service.PomodoroPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 番茄钟计划控制器
 *
 * <p>所属模块：番茄钟模块-计划管理</p>
 * <p>API路径前缀：/api/pomodoro/plans</p>
 * <p>功能描述：提供番茄钟计划的增删改查、启用计划列表、默认计划等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/pomodoro/plans")
@RequiredArgsConstructor
public class PomodoroPlanController {

    private final PomodoroPlanService pomodoroPlanService;

    /**
     * 分页查询番茄钟计划列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/plans</p>
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @return 番茄钟计划分页结果
     */
    @GetMapping
    public ApiResult<PageResult<PomodoroPlan>> list(@RequestParam(required = false) Long page,
                                                    @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(pomodoroPlanService.pagePlans(page, pageSize));
    }

    /**
     * 获取已启用的番茄钟计划列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/plans/enabled</p>
     *
     * @return 已启用的番茄钟计划列表
     */
    @GetMapping("/enabled")
    public ApiResult<List<PomodoroPlan>> listEnabled() {
        return ApiResult.ok(pomodoroPlanService.listEnabled());
    }

    /**
     * 获取默认番茄钟计划
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/plans/default</p>
     *
     * @return 默认番茄钟计划
     */
    @GetMapping("/default")
    public ApiResult<PomodoroPlan> getDefault() {
        return ApiResult.ok(pomodoroPlanService.getDefaultPlan());
    }

    /**
     * 获取番茄钟计划详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/plans/{id}</p>
     *
     * @param id 计划ID
     * @return 番茄钟计划详情
     */
    @GetMapping("/{id}")
    public ApiResult<PomodoroPlan> get(@PathVariable Long id) {
        PomodoroPlan plan = pomodoroPlanService.getById(id);
        if (plan == null) {
            return ApiResult.fail(com.ai.manager.common.result.ResultCode.NOT_FOUND);
        }
        return ApiResult.ok(plan);
    }

    /**
     * 创建番茄钟计划
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/pomodoro/plans</p>
     *
     * @param request 番茄钟计划保存请求参数
     * @return 创建后的番茄钟计划
     */
    @PostMapping
    public ApiResult<PomodoroPlan> create(@RequestBody PomodoroPlanSaveRequest request) {
        return ApiResult.ok(pomodoroPlanService.createPlan(request));
    }

    /**
     * 更新番茄钟计划
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/pomodoro/plans/{id}</p>
     *
     * @param id 计划ID
     * @param request 番茄钟计划保存请求参数
     * @return 更新后的番茄钟计划
     */
    @PutMapping("/{id}")
    public ApiResult<PomodoroPlan> update(@PathVariable Long id, @RequestBody PomodoroPlanSaveRequest request) {
        return ApiResult.ok(pomodoroPlanService.updatePlan(id, request));
    }

    /**
     * 删除番茄钟计划
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/pomodoro/plans/{id}</p>
     *
     * @param id 计划ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        pomodoroPlanService.deletePlan(id);
        return ApiResult.ok();
    }
}
