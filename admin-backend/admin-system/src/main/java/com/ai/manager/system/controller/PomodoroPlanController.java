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

@RestController
@RequestMapping("/api/pomodoro/plans")
@RequiredArgsConstructor
public class PomodoroPlanController {

    private final PomodoroPlanService pomodoroPlanService;

    @GetMapping
    public ApiResult<PageResult<PomodoroPlan>> list(@RequestParam(required = false) Long page,
                                                    @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(pomodoroPlanService.pagePlans(page, pageSize));
    }

    @GetMapping("/enabled")
    public ApiResult<List<PomodoroPlan>> listEnabled() {
        return ApiResult.ok(pomodoroPlanService.listEnabled());
    }

    @GetMapping("/default")
    public ApiResult<PomodoroPlan> getDefault() {
        return ApiResult.ok(pomodoroPlanService.getDefaultPlan());
    }

    @GetMapping("/{id}")
    public ApiResult<PomodoroPlan> get(@PathVariable Long id) {
        PomodoroPlan plan = pomodoroPlanService.getById(id);
        if (plan == null) {
            return ApiResult.fail(com.ai.manager.common.result.ResultCode.NOT_FOUND);
        }
        return ApiResult.ok(plan);
    }

    @PostMapping
    public ApiResult<PomodoroPlan> create(@RequestBody PomodoroPlanSaveRequest request) {
        return ApiResult.ok(pomodoroPlanService.createPlan(request));
    }

    @PutMapping("/{id}")
    public ApiResult<PomodoroPlan> update(@PathVariable Long id, @RequestBody PomodoroPlanSaveRequest request) {
        return ApiResult.ok(pomodoroPlanService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        pomodoroPlanService.deletePlan(id);
        return ApiResult.ok();
    }
}
