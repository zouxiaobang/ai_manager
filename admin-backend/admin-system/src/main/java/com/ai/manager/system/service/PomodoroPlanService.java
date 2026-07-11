package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.PomodoroPlanSaveRequest;
import com.ai.manager.system.domain.entity.PomodoroPlan;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 番茄钟计划服务接口
 *
 * <p>提供番茄钟计划的分页查询、启用列表、默认计划、创建、更新和删除等番茄钟计划管理功能。</p>
 */
public interface PomodoroPlanService extends IService<PomodoroPlan> {

    /**
     * 分页查询番茄钟计划列表
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 番茄钟计划分页结果
     */
    PageResult<PomodoroPlan> pagePlans(Long page, Long pageSize);

    /**
     * 查询启用的番茄钟计划列表
     *
     * @return 启用的番茄钟计划列表
     */
    List<PomodoroPlan> listEnabled();

    /**
     * 获取默认番茄钟计划
     *
     * @return 默认番茄钟计划
     */
    PomodoroPlan getDefaultPlan();

    /**
     * 创建番茄钟计划
     *
     * @param request 计划保存请求参数
     * @return 创建后的番茄钟计划
     */
    PomodoroPlan createPlan(PomodoroPlanSaveRequest request);

    /**
     * 更新番茄钟计划
     *
     * @param id      计划ID
     * @param request 计划保存请求参数
     * @return 更新后的番茄钟计划
     */
    PomodoroPlan updatePlan(Long id, PomodoroPlanSaveRequest request);

    /**
     * 删除番茄钟计划
     *
     * @param id 计划ID
     */
    void deletePlan(Long id);
}
