package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcOutboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcOutboundOrderSaveRequest;
import com.ai.manager.system.domain.vo.EcOutboundOrderDetailVO;
import com.ai.manager.system.service.EcOutboundOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 电商出库单控制器
 *
 * <p>所属模块：电商模块-出库管理</p>
 * <p>API路径前缀：/api/ecommerce/outbound-orders</p>
 * <p>功能描述：提供出库单的增删改查、确认、取消等出库单管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/outbound-orders")
@RequiredArgsConstructor
public class EcOutboundOrderController {

    private final EcOutboundOrderService ecOutboundOrderService;

    /**
     * 分页查询出库单列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/outbound-orders</p>
     *
     * @param keyword 关键词，用于搜索出库单号等
     * @param status 出库单状态
     * @param factoryId 工厂ID
     * @param page 页码
     * @param pageSize 每页条数
     * @return 出库单分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcOutboundOrderDetailVO>> list(@RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) Long factoryId,
                                                              @RequestParam(required = false) Long page,
                                                              @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecOutboundOrderService.pageOrders(keyword, status, factoryId, page, pageSize));
    }

    /**
     * 获取出库单详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/outbound-orders/{id}</p>
     *
     * @param id 出库单ID
     * @return 出库单详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcOutboundOrderDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecOutboundOrderService.getOrderDetail(id));
    }

    /**
     * 创建出库单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/outbound-orders</p>
     *
     * @param request 出库单保存请求参数
     * @return 创建后的出库单详情
     */
    @PostMapping
    public ApiResult<EcOutboundOrderDetailVO> create(@jakarta.validation.Valid @RequestBody EcOutboundOrderSaveRequest request) {
        return ApiResult.ok(ecOutboundOrderService.createOrder(request));
    }

    /**
     * 更新出库单
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/outbound-orders/{id}</p>
     *
     * @param id 出库单ID
     * @param request 出库单保存请求参数
     * @return 更新后的出库单详情
     */
    @PutMapping("/{id}")
    public ApiResult<EcOutboundOrderDetailVO> update(@PathVariable Long id,
                                                     @jakarta.validation.Valid @RequestBody EcOutboundOrderSaveRequest request) {
        return ApiResult.ok(ecOutboundOrderService.updateOrder(id, request));
    }

    /**
     * 确认出库单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/outbound-orders/{id}/confirm</p>
     *
     * @param id 出库单ID
     * @param request 出库单确认请求参数
     * @return 确认后的出库单详情
     */
    @PostMapping("/{id}/confirm")
    public ApiResult<EcOutboundOrderDetailVO> confirm(@PathVariable Long id,
                                                      @jakarta.validation.Valid @RequestBody EcOutboundOrderConfirmRequest request) {
        return ApiResult.ok(ecOutboundOrderService.confirmOrder(id, request));
    }

    /**
     * 取消出库单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/outbound-orders/{id}/cancel</p>
     *
     * @param id 出库单ID
     * @return 操作结果
     */
    @PostMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        ecOutboundOrderService.cancelOrder(id);
        return ApiResult.ok();
    }

    /**
     * 删除出库单
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/outbound-orders/{id}</p>
     *
     * @param id 出库单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecOutboundOrderService.deleteOrder(id);
        return ApiResult.ok();
    }
}
