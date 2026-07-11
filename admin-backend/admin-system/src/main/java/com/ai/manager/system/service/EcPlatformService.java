package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcPlatformSaveRequest;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.vo.EcPlatformListItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 电商平台服务接口
 *
 * <p>提供电商平台的分页查询、选项列表、详情查看、创建、更新和删除等基础CRUD功能。</p>
 */
public interface EcPlatformService extends IService<EcPlatform> {

    /**
     * 分页查询平台列表
     *
     * @param keyword     关键词（平台名称等）
     * @param channelType 渠道类型
     * @param page        页码
     * @param pageSize    每页条数
     * @return 平台分页结果
     */
    PageResult<EcPlatformListItemVO> pagePlatforms(String keyword, String channelType, Long page, Long pageSize);

    /**
     * 查询平台选项列表
     *
     * @return 平台选项列表
     */
    List<EcPlatformListItemVO> listPlatformOptions();

    /**
     * 获取平台详情
     *
     * @param id 平台ID
     * @return 平台详情信息
     */
    EcPlatformListItemVO getPlatformDetail(Long id);

    /**
     * 创建平台
     *
     * @param request 平台保存请求参数
     * @return 创建后的平台信息
     */
    EcPlatformListItemVO createPlatform(EcPlatformSaveRequest request);

    /**
     * 更新平台
     *
     * @param id      平台ID
     * @param request 平台保存请求参数
     * @return 更新后的平台信息
     */
    EcPlatformListItemVO updatePlatform(Long id, EcPlatformSaveRequest request);

    /**
     * 删除平台
     *
     * @param id 平台ID
     */
    void deletePlatform(Long id);
}
