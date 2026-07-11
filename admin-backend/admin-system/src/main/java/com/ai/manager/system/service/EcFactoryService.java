package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcFactorySaveRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.vo.EcFactoryStatsVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 电商工厂服务接口
 *
 * <p>提供电商工厂的分页查询、统计数据、选项列表、创建、更新和删除等基础CRUD功能。</p>
 */
public interface EcFactoryService extends IService<EcFactory> {

    /**
     * 分页查询工厂列表
     *
     * @param keyword     关键词（工厂名称等）
     * @param factoryType 工厂类型
     * @param status      状态
     * @param page        页码
     * @param pageSize    每页条数
     * @return 工厂分页结果
     */
    PageResult<EcFactory> pageFactories(String keyword, String factoryType, String status, Long page, Long pageSize);

    /**
     * 获取工厂统计数据
     *
     * @return 工厂统计信息
     */
    EcFactoryStatsVO getFactoryStats();

    /**
     * 查询工厂选项列表
     *
     * @param factoryType 工厂类型
     * @return 工厂选项列表
     */
    List<EcFactory> listFactoryOptions(String factoryType);

    /**
     * 创建工厂
     *
     * @param request 工厂保存请求参数
     * @return 创建后的工厂信息
     */
    EcFactory createFactory(EcFactorySaveRequest request);

    /**
     * 更新工厂
     *
     * @param id      工厂ID
     * @param request 工厂保存请求参数
     * @return 更新后的工厂信息
     */
    EcFactory updateFactory(Long id, EcFactorySaveRequest request);

    /**
     * 删除工厂
     *
     * @param id 工厂ID
     */
    void deleteFactory(Long id);
}
