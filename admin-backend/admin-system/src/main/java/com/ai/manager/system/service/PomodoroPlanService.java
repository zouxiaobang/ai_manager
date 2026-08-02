package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.PomodoroPlanSaveRequest;
import com.ai.manager.system.domain.entity.PomodoroPlan;
import com.ai.manager.system.domain.vo.PomodoroPlanVO;
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
    PageResult<PomodoroPlanVO> pagePlans(Long page, Long pageSize);

    /**
     * 查询启用的番茄钟计划列表
     *
     * @return 启用的番茄钟计划列表
     */
    List<PomodoroPlanVO> listEnabled();

    /**
     * 获取默认番茄钟计划
     *
     * @return 默认番茄钟计划
     */
    PomodoroPlanVO getDefaultPlan();

    /**
     * 查询番茄钟计划详情（响应 VO）
     *
     * @param id 计划ID
     * @return 计划详情，不存在返回 null
     */
    PomodoroPlanVO getPlan(Long id);

    /**
     * 创建番茄钟计划
     *
     * @param request 计划保存请求参数
     * @return 创建后的番茄钟计划
     */
    PomodoroPlanVO createPlan(PomodoroPlanSaveRequest request);

    /**
     * 更新番茄钟计划
     *
     * @param id      计划ID
     * @param request 计划保存请求参数
     * @return 更新后的番茄钟计划
     */
    PomodoroPlanVO updatePlan(Long id, PomodoroPlanSaveRequest request);

    /**
     * 删除番茄钟计划
     *
     * @param id 计划ID
     */
    void deletePlan(Long id);
}
