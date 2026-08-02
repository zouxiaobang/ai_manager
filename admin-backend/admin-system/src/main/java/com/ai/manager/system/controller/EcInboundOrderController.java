package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcInboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcInboundOrderSaveRequest;
import com.ai.manager.system.domain.vo.EcInboundOrderDetailVO;
import com.ai.manager.system.service.EcInboundOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电商入库单控制器
 *
 * <p>所属模块：电商模块-入库管理</p>
 * <p>API路径前缀：/api/ecommerce/inbound-orders</p>
 * <p>功能描述：提供入库单的增删改查、确认、取消等入库单管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/inbound-orders")
@RequiredArgsConstructor
public class EcInboundOrderController {

    private final EcInboundOrderService ecInboundOrderService;

    /**
     * 分页查询入库单列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inbound-orders</p>
     *
     * @param keyword 关键词，用于搜索入库单号等
     * @param status 入库单状态
     * @param factoryId 工厂ID
     * @param orderMonth 订单月份
     * @param page 页码
     * @param pageSize 每页条数
     * @return 入库单分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcInboundOrderDetailVO>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Long factoryId,
                                                               @RequestParam(required = false) String orderMonth,
                                                               @RequestParam(required = false) Long page,
                                                               @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecInboundOrderService.pageOrders(keyword, status, factoryId, orderMonth, page, pageSize));
    }

    /**
     * 获取入库单详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inbound-orders/{id}</p>
     *
     * @param id 入库单ID
     * @return 入库单详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcInboundOrderDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecInboundOrderService.getOrderDetail(id));
    }

    /**
     * 创建入库单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/inbound-orders</p>
     *
     * @param request 入库单保存请求参数
     * @return 创建后的入库单详情
     */
    @PostMapping
    public ApiResult<EcInboundOrderDetailVO> create(@jakarta.validation.Valid @RequestBody EcInboundOrderSaveRequest request) {
        return ApiResult.ok(ecInboundOrderService.createOrder(request));
    }

    /**
     * 更新入库单
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/inbound-orders/{id}</p>
     *
     * @param id 入库单ID
     * @param request 入库单保存请求参数
     * @return 更新后的入库单详情
     */
    @PutMapping("/{id}")
    public ApiResult<EcInboundOrderDetailVO> update(@PathVariable Long id,
                                                    @jakarta.validation.Valid @RequestBody EcInboundOrderSaveRequest request) {
        return ApiResult.ok(ecInboundOrderService.updateOrder(id, request));
    }

    /**
     * 确认入库单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/inbound-orders/{id}/confirm</p>
     *
     * @param id 入库单ID
     * @param request 入库单确认请求参数
     * @return 确认后的入库单详情
     */
    @PostMapping("/{id}/confirm")
    public ApiResult<EcInboundOrderDetailVO> confirm(@PathVariable Long id,
                                                   @jakarta.validation.Valid @RequestBody EcInboundOrderConfirmRequest request) {
        return ApiResult.ok(ecInboundOrderService.confirmOrder(id, request));
    }

    /**
     * 取消入库单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/inbound-orders/{id}/cancel</p>
     *
     * @param id 入库单ID
     * @return 操作结果
     */
    @PostMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        ecInboundOrderService.cancelOrder(id);
        return ApiResult.ok();
    }

    /**
     * 删除入库单
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/inbound-orders/{id}</p>
     *
     * @param id 入库单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecInboundOrderService.deleteOrder(id);
        return ApiResult.ok();
    }
}
