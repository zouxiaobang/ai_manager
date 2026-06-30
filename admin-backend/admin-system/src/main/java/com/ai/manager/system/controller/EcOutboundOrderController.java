package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcOutboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcOutboundOrderSaveRequest;
import com.ai.manager.system.domain.vo.EcOutboundOrderDetailVO;
import com.ai.manager.system.service.EcOutboundOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/outbound-orders")
@RequiredArgsConstructor
public class EcOutboundOrderController {

    private final EcOutboundOrderService ecOutboundOrderService;

    @GetMapping
    public ApiResult<PageResult<EcOutboundOrderDetailVO>> list(@RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) Long factoryId,
                                                              @RequestParam(required = false) Long page,
                                                              @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecOutboundOrderService.pageOrders(keyword, status, factoryId, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResult<EcOutboundOrderDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecOutboundOrderService.getOrderDetail(id));
    }

    @PostMapping
    public ApiResult<EcOutboundOrderDetailVO> create(@RequestBody EcOutboundOrderSaveRequest request) {
        return ApiResult.ok(ecOutboundOrderService.createOrder(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcOutboundOrderDetailVO> update(@PathVariable Long id,
                                                     @RequestBody EcOutboundOrderSaveRequest request) {
        return ApiResult.ok(ecOutboundOrderService.updateOrder(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResult<EcOutboundOrderDetailVO> confirm(@PathVariable Long id,
                                                      @RequestBody EcOutboundOrderConfirmRequest request) {
        return ApiResult.ok(ecOutboundOrderService.confirmOrder(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        ecOutboundOrderService.cancelOrder(id);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecOutboundOrderService.deleteOrder(id);
        return ApiResult.ok();
    }
}
