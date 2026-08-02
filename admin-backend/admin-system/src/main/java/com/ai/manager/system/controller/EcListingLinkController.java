package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcListingLinkPricingRequest;
import com.ai.manager.system.domain.dto.EcListingLinkSaveRequest;
import com.ai.manager.system.domain.vo.EcListingLinkDetailVO;
import com.ai.manager.system.domain.vo.EcListingLinkPricingVO;
import com.ai.manager.system.service.EcListingLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 电商Listing链接控制器
 *
 * <p>所属模块：电商模块-Listing管理</p>
 * <p>API路径前缀：/api/ecommerce/listing-links</p>
 * <p>功能描述：提供Listing链接的增删改查、复制、定价计算、批量重新计算等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/listing-links")
@RequiredArgsConstructor
public class EcListingLinkController {

    private final EcListingLinkService ecListingLinkService;

    /**
     * 分页查询Listing链接列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/listing-links</p>
     *
     * @param keyword 关键词，用于搜索链接名称等
     * @param shopId 店铺ID
     * @param platformId 平台ID
     * @param page 页码
     * @param pageSize 每页条数
     * @return Listing链接分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcListingLinkDetailVO>> list(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Long shopId,
                                                             @RequestParam(required = false) Long platformId,
                                                             @RequestParam(required = false) Long page,
                                                             @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecListingLinkService.pageLinks(keyword, shopId, platformId, page, pageSize));
    }

    /**
     * 根据商品查询Listing链接列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/listing-links/by-product/{productId}</p>
     *
     * @param productId 商品ID
     * @return Listing链接列表
     */
    @GetMapping("/by-product/{productId}")
    public ApiResult<List<EcListingLinkDetailVO>> listByProduct(@PathVariable Long productId) {
        return ApiResult.ok(ecListingLinkService.listLinksByProductId(productId));
    }

    /**
     * 计算定价
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/listing-links/calculate-pricing</p>
     *
     * @param request 定价计算请求参数
     * @return 定价计算结果
     */
    @PostMapping("/calculate-pricing")
    public ApiResult<EcListingLinkPricingVO> calculatePricing(@jakarta.validation.Valid @RequestBody EcListingLinkPricingRequest request) {
        return ApiResult.ok(ecListingLinkService.calculatePricing(request));
    }

    /**
     * 重新计算所有定价
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/listing-links/recalculate-all</p>
     *
     * @return 更新数量
     */
    @PostMapping("/recalculate-all")
    public ApiResult<Map<String, Integer>> recalculateAll() {
        int updated = ecListingLinkService.recalculateAllPricing();
        return ApiResult.ok(Map.of("updated", updated));
    }

    /**
     * 获取Listing链接详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/listing-links/{id}</p>
     *
     * @param id 链接ID
     * @return Listing链接详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcListingLinkDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecListingLinkService.getLinkDetail(id));
    }

    /**
     * 创建Listing链接
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/listing-links</p>
     *
     * @param request Listing链接保存请求参数
     * @return 创建后的Listing链接详情
     */
    @PostMapping
    public ApiResult<EcListingLinkDetailVO> create(@jakarta.validation.Valid @RequestBody EcListingLinkSaveRequest request) {
        return ApiResult.ok(ecListingLinkService.createLink(request));
    }

    /**
     * 复制Listing链接
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/listing-links/{id}/copy</p>
     *
     * @param id 链接ID
     * @return 复制后的Listing链接详情
     */
    @PostMapping("/{id}/copy")
    public ApiResult<EcListingLinkDetailVO> copy(@PathVariable Long id) {
        return ApiResult.ok(ecListingLinkService.copyLink(id));
    }

    /**
     * 更新Listing链接
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/listing-links/{id}</p>
     *
     * @param id 链接ID
     * @param request Listing链接保存请求参数
     * @return 更新后的Listing链接详情
     */
    @PutMapping("/{id}")
    public ApiResult<EcListingLinkDetailVO> update(@PathVariable Long id,
                                                   @jakarta.validation.Valid @RequestBody EcListingLinkSaveRequest request) {
        return ApiResult.ok(ecListingLinkService.updateLink(id, request));
    }

    /**
     * 删除Listing链接
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/listing-links/{id}</p>
     *
     * @param id 链接ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecListingLinkService.deleteLink(id);
        return ApiResult.ok();
    }
}
