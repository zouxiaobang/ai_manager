package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcShopSaveRequest;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.vo.EcShopListItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 电商店铺服务接口
 *
 * <p>提供电商店铺的分页查询、选项列表、详情查看、创建、更新和删除等基础CRUD功能。</p>
 */
public interface EcShopService extends IService<EcShop> {

    /**
     * 分页查询店铺列表
     *
     * @param keyword    关键词（店铺名称等）
     * @param platformId 平台ID
     * @param page       页码
     * @param pageSize   每页条数
     * @return 店铺分页结果
     */
    PageResult<EcShopListItemVO> pageShops(String keyword, Long platformId, Long page, Long pageSize);

    /**
     * 查询店铺选项列表
     *
     * @param platformId 平台ID
     * @return 店铺选项列表
     */
    List<EcShopListItemVO> listShopOptions(Long platformId);

    /**
     * 获取店铺详情
     *
     * @param id 店铺ID
     * @return 店铺详情信息
     */
    EcShopListItemVO getShopDetail(Long id);

    /**
     * 创建店铺
     *
     * @param request 店铺保存请求参数
     * @return 创建后的店铺信息
     */
    EcShopListItemVO createShop(EcShopSaveRequest request);

    /**
     * 更新店铺
     *
     * @param id      店铺ID
     * @param request 店铺保存请求参数
     * @return 更新后的店铺信息
     */
    EcShopListItemVO updateShop(Long id, EcShopSaveRequest request);

    /**
     * 删除店铺
     *
     * @param id 店铺ID
     */
    void deleteShop(Long id);
}
