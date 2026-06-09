package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.PomodoroPlanSaveRequest;
import com.ai.manager.system.domain.entity.PomodoroPlan;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PomodoroPlanService extends IService<PomodoroPlan> {

    PageResult<PomodoroPlan> pagePlans(Long page, Long pageSize);

    List<PomodoroPlan> listEnabled();

    PomodoroPlan getDefaultPlan();

    PomodoroPlan createPlan(PomodoroPlanSaveRequest request);

    PomodoroPlan updatePlan(Long id, PomodoroPlanSaveRequest request);

    void deletePlan(Long id);
}
