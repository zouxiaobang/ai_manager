package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.PomodoroPlanSaveRequest;
import com.ai.manager.system.domain.entity.PomodoroPlan;
import com.ai.manager.system.mapper.PomodoroPlanMapper;
import com.ai.manager.system.service.PomodoroPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PomodoroPlanServiceImpl extends ServiceImpl<PomodoroPlanMapper, PomodoroPlan>
        implements PomodoroPlanService {

    @Override
    public PageResult<PomodoroPlan> pagePlans(Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        Page<PomodoroPlan> entityPage = page(new Page<>(p, ps), new LambdaQueryWrapper<PomodoroPlan>()
                .orderByDesc(PomodoroPlan::getIsDefault)
                .orderByDesc(PomodoroPlan::getId));
        return PageUtils.of(entityPage.getRecords(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public List<PomodoroPlan> listEnabled() {
        return list(new LambdaQueryWrapper<PomodoroPlan>()
                .eq(PomodoroPlan::getStatus, "ENABLED")
                .orderByDesc(PomodoroPlan::getIsDefault)
                .orderByDesc(PomodoroPlan::getId));
    }

    @Override
    public PomodoroPlan getDefaultPlan() {
        PomodoroPlan plan = getOne(new LambdaQueryWrapper<PomodoroPlan>()
                .eq(PomodoroPlan::getIsDefault, 1)
                .eq(PomodoroPlan::getStatus, "ENABLED")
                .last("LIMIT 1"));
        if (plan != null) {
            return plan;
        }
        List<PomodoroPlan> enabled = listEnabled();
        if (enabled.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "暂无可用番茄钟计划");
        }
        return enabled.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PomodoroPlan createPlan(PomodoroPlanSaveRequest request) {
        validatePlanRequest(request);
        PomodoroPlan plan = toEntity(request, new PomodoroPlan());
        if (Boolean.TRUE.equals(request.getAsDefault())) {
            clearDefaultFlag();
            plan.setIsDefault(1);
        } else {
            plan.setIsDefault(0);
        }
        save(plan);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PomodoroPlan updatePlan(Long id, PomodoroPlanSaveRequest request) {
        PomodoroPlan existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validatePlanRequest(request);
        PomodoroPlan plan = toEntity(request, existing);
        if (Boolean.TRUE.equals(request.getAsDefault())) {
            clearDefaultFlag();
            plan.setIsDefault(1);
        }
        updateById(plan);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        PomodoroPlan plan = getById(id);
        if (plan == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (plan.getIsDefault() != null && plan.getIsDefault() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能删除默认计划，请先指定其他默认计划");
        }
        removeById(id);
    }

    private void clearDefaultFlag() {
        update(new LambdaUpdateWrapper<PomodoroPlan>().set(PomodoroPlan::getIsDefault, 0));
    }

    private void validatePlanRequest(PomodoroPlanSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "计划名称不能为空");
        }
        if (request.getWorkDurationMin() == null || request.getWorkDurationMin() < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "专注时长至少 1 分钟");
        }
    }

    private PomodoroPlan toEntity(PomodoroPlanSaveRequest request, PomodoroPlan plan) {
        plan.setTitle(request.getTitle().trim());
        plan.setWorkDurationMin(defaultInt(request.getWorkDurationMin(), 25));
        plan.setShortBreakMin(defaultInt(request.getShortBreakMin(), 5));
        plan.setLongBreakMin(defaultInt(request.getLongBreakMin(), 15));
        plan.setRoundsBeforeLongBreak(defaultInt(request.getRoundsBeforeLongBreak(), 4));
        plan.setDailyGoalRounds(defaultInt(request.getDailyGoalRounds(), 8));
        plan.setDailyGoalMinutes(defaultInt(request.getDailyGoalMinutes(), 200));
        if (StringUtils.hasText(request.getStatus())) {
            plan.setStatus(request.getStatus());
        } else if (plan.getStatus() == null) {
            plan.setStatus("ENABLED");
        }
        return plan;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
