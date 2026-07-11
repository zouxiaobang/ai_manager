package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcShopSaveRequest;
import com.ai.manager.system.domain.vo.EcShopListItemVO;
import com.ai.manager.system.service.EcShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电商店铺控制器
 *
 * <p>所属模块：电商模块-店铺管理</p>
 * <p>API路径前缀：/api/ecommerce/shops</p>
 * <p>功能描述：提供店铺的增删改查、店铺选项列表等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/shops")
@RequiredArgsConstructor
public class EcShopController {

    private final EcShopService ecShopService;

    /**
     * 分页查询店铺列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/shops</p>
     *
     * @param keyword 关键词，用于搜索店铺名称等
     * @param platformId 平台ID
     * @param page 页码
     * @param pageSize 每页条数
     * @return 店铺分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcShopListItemVO>> list(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Long platformId,
                                                          @RequestParam(required = false) Long page,
                                                          @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecShopService.pageShops(keyword, platformId, page, pageSize));
    }

    /**
     * 获取店铺选项列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/shops/options</p>
     *
     * @param platformId 平台ID
     * @return 店铺选项列表
     */
    @GetMapping("/options")
    public ApiResult<List<EcShopListItemVO>> options(@RequestParam(required = false) Long platformId) {
        return ApiResult.ok(ecShopService.listShopOptions(platformId));
    }

    /**
     * 获取店铺详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/shops/{id}</p>
     *
     * @param id 店铺ID
     * @return 店铺详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcShopListItemVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecShopService.getShopDetail(id));
    }

    /**
     * 创建店铺
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/shops</p>
     *
     * @param request 店铺保存请求参数
     * @return 创建后的店铺信息
     */
    @PostMapping
    public ApiResult<EcShopListItemVO> create(@RequestBody EcShopSaveRequest request) {
        return ApiResult.ok(ecShopService.createShop(request));
    }

    /**
     * 更新店铺
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/shops/{id}</p>
     *
     * @param id 店铺ID
     * @param request 店铺保存请求参数
     * @return 更新后的店铺信息
     */
    @PutMapping("/{id}")
    public ApiResult<EcShopListItemVO> update(@PathVariable Long id, @RequestBody EcShopSaveRequest request) {
        return ApiResult.ok(ecShopService.updateShop(id, request));
    }

    /**
     * 删除店铺
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/shops/{id}</p>
     *
     * @param id 店铺ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecShopService.deleteShop(id);
        return ApiResult.ok();
    }
}
