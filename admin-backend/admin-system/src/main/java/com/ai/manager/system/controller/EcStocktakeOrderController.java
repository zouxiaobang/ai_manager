package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcStocktakeOrderSaveRequest;
import com.ai.manager.system.domain.vo.EcStocktakeOrderDetailVO;
import com.ai.manager.system.service.EcStocktakeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 电商盘点单控制器
 *
 * <p>所属模块：电商模块-盘点管理</p>
 * <p>API路径前缀：/api/ecommerce/stocktake-orders</p>
 * <p>功能描述：提供盘点单的增删改查、确认、取消等盘点单管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/stocktake-orders")
@RequiredArgsConstructor
public class EcStocktakeOrderController {

    private final EcStocktakeOrderService ecStocktakeOrderService;

    /**
     * 分页查询盘点单列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/stocktake-orders</p>
     *
     * @param keyword 关键词，用于搜索盘点单号等
     * @param status 盘点单状态
     * @param factoryId 工厂ID
     * @param page 页码
     * @param pageSize 每页条数
     * @return 盘点单分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcStocktakeOrderDetailVO>> list(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = false) Long factoryId,
                                                                @RequestParam(required = false) Long page,
                                                                @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecStocktakeOrderService.pageOrders(keyword, status, factoryId, page, pageSize));
    }

    /**
     * 获取盘点单详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/stocktake-orders/{id}</p>
     *
     * @param id 盘点单ID
     * @return 盘点单详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcStocktakeOrderDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecStocktakeOrderService.getOrderDetail(id));
    }

    /**
     * 创建盘点单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/stocktake-orders</p>
     *
     * @param request 盘点单保存请求参数
     * @return 创建后的盘点单详情
     */
    @PostMapping
    public ApiResult<EcStocktakeOrderDetailVO> create(@RequestBody EcStocktakeOrderSaveRequest request) {
        return ApiResult.ok(ecStocktakeOrderService.createOrder(request));
    }

    /**
     * 更新盘点单
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/stocktake-orders/{id}</p>
     *
     * @param id 盘点单ID
     * @param request 盘点单保存请求参数
     * @return 更新后的盘点单详情
     */
    @PutMapping("/{id}")
    public ApiResult<EcStocktakeOrderDetailVO> update(@PathVariable Long id,
                                                      @RequestBody EcStocktakeOrderSaveRequest request) {
        return ApiResult.ok(ecStocktakeOrderService.updateOrder(id, request));
    }

    /**
     * 确认盘点单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/stocktake-orders/{id}/confirm</p>
     *
     * @param id 盘点单ID
     * @return 确认后的盘点单详情
     */
    @PostMapping("/{id}/confirm")
    public ApiResult<EcStocktakeOrderDetailVO> confirm(@PathVariable Long id) {
        return ApiResult.ok(ecStocktakeOrderService.confirmOrder(id));
    }

    /**
     * 取消盘点单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/stocktake-orders/{id}/cancel</p>
     *
     * @param id 盘点单ID
     * @return 操作结果
     */
    @PostMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        ecStocktakeOrderService.cancelOrder(id);
        return ApiResult.ok();
    }

    /**
     * 删除盘点单
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/stocktake-orders/{id}</p>
     *
     * @param id 盘点单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecStocktakeOrderService.deleteOrder(id);
        return ApiResult.ok();
    }
}
