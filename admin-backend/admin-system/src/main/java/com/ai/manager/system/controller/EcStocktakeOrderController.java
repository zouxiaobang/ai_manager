package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcStocktakeOrderSaveRequest;
import com.ai.manager.system.domain.vo.EcStocktakeOrderDetailVO;
import com.ai.manager.system.service.EcStocktakeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/stocktake-orders")
@RequiredArgsConstructor
public class EcStocktakeOrderController {

    private final EcStocktakeOrderService ecStocktakeOrderService;

    @GetMapping
    public ApiResult<PageResult<EcStocktakeOrderDetailVO>> list(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = false) Long factoryId,
                                                                @RequestParam(required = false) Long page,
                                                                @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecStocktakeOrderService.pageOrders(keyword, status, factoryId, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResult<EcStocktakeOrderDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecStocktakeOrderService.getOrderDetail(id));
    }

    @PostMapping
    public ApiResult<EcStocktakeOrderDetailVO> create(@RequestBody EcStocktakeOrderSaveRequest request) {
        return ApiResult.ok(ecStocktakeOrderService.createOrder(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcStocktakeOrderDetailVO> update(@PathVariable Long id,
                                                      @RequestBody EcStocktakeOrderSaveRequest request) {
        return ApiResult.ok(ecStocktakeOrderService.updateOrder(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResult<EcStocktakeOrderDetailVO> confirm(@PathVariable Long id) {
        return ApiResult.ok(ecStocktakeOrderService.confirmOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        ecStocktakeOrderService.cancelOrder(id);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecStocktakeOrderService.deleteOrder(id);
        return ApiResult.ok();
    }
}
