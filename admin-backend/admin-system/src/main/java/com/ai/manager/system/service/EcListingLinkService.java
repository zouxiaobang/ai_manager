package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcListingLinkPricingRequest;
import com.ai.manager.system.domain.dto.EcListingLinkSaveRequest;
import com.ai.manager.system.domain.entity.EcListingLink;
import com.ai.manager.system.domain.vo.EcListingLinkDetailVO;
import com.ai.manager.system.domain.vo.EcListingLinkPricingVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 电商上架链接服务接口
 *
 * <p>提供电商上架链接的分页查询、按商品查询、详情查看、创建、更新、复制、删除、
 * 定价计算和全量重新定价等上架链接管理功能。</p>
 */
public interface EcListingLinkService extends IService<EcListingLink> {

    /**
     * 分页查询上架链接列表
     *
     * @param keyword    关键词（链接名称等）
     * @param shopId     店铺ID
     * @param platformId 平台ID
     * @param page       页码
     * @param pageSize   每页条数
     * @return 上架链接分页结果
     */
    PageResult<EcListingLinkDetailVO> pageLinks(String keyword, Long shopId, Long platformId,
                                                Long page, Long pageSize);

    /**
     * 根据商品ID查询上架链接列表
     *
     * @param productId 商品ID
     * @return 上架链接列表
     */
    List<EcListingLinkDetailVO> listLinksByProductId(Long productId);

    /**
     * 获取上架链接详情
     *
     * @param id 链接ID
     * @return 上架链接详情信息
     */
    EcListingLinkDetailVO getLinkDetail(Long id);

    /**
     * 创建上架链接
     *
     * @param request 链接保存请求参数
     * @return 创建后的上架链接详情
     */
    EcListingLinkDetailVO createLink(EcListingLinkSaveRequest request);

    /**
     * 更新上架链接
     *
     * @param id      链接ID
     * @param request 链接保存请求参数
     * @return 更新后的上架链接详情
     */
    EcListingLinkDetailVO updateLink(Long id, EcListingLinkSaveRequest request);

    /**
     * 复制上架链接
     *
     * @param id 链接ID
     * @return 复制后的上架链接详情
     */
    EcListingLinkDetailVO copyLink(Long id);

    /**
     * 删除上架链接
     *
     * @param id 链接ID
     */
    void deleteLink(Long id);

    /**
     * 计算定价
     *
     * @param request 定价计算请求参数
     * @return 定价计算结果
     */
    EcListingLinkPricingVO calculatePricing(EcListingLinkPricingRequest request);

    /**
     * 重新计算所有定价
     *
     * @return 重新计算的数量
     */
    int recalculateAllPricing();
}
