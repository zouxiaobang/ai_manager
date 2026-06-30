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

@RestController
@RequestMapping("/api/ecommerce/inbound-orders")
@RequiredArgsConstructor
public class EcInboundOrderController {

    private final EcInboundOrderService ecInboundOrderService;

    @GetMapping
    public ApiResult<PageResult<EcInboundOrderDetailVO>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Long factoryId,
                                                               @RequestParam(required = false) String orderMonth,
                                                               @RequestParam(required = false) Long page,
                                                               @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecInboundOrderService.pageOrders(keyword, status, factoryId, orderMonth, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResult<EcInboundOrderDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecInboundOrderService.getOrderDetail(id));
    }

    @PostMapping
    public ApiResult<EcInboundOrderDetailVO> create(@RequestBody EcInboundOrderSaveRequest request) {
        return ApiResult.ok(ecInboundOrderService.createOrder(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcInboundOrderDetailVO> update(@PathVariable Long id,
                                                    @RequestBody EcInboundOrderSaveRequest request) {
        return ApiResult.ok(ecInboundOrderService.updateOrder(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResult<EcInboundOrderDetailVO> confirm(@PathVariable Long id,
                                                   @RequestBody EcInboundOrderConfirmRequest request) {
        return ApiResult.ok(ecInboundOrderService.confirmOrder(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        ecInboundOrderService.cancelOrder(id);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecInboundOrderService.deleteOrder(id);
        return ApiResult.ok();
    }
}
