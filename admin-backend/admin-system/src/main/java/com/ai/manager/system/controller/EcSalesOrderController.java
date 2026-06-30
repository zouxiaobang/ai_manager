package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcSalesOrderImportManualCostUpdateRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderImportPreviewRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderLineRefundRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderSaveRequest;
import com.ai.manager.system.domain.vo.EcSalesOrderDetailVO;
import com.ai.manager.system.domain.vo.EcSalesOrderImportPreviewVO;
import com.ai.manager.system.domain.vo.EcSalesOrderMonthlyOverviewVO;
import com.ai.manager.system.service.EcSalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ecommerce/sales-orders")
@RequiredArgsConstructor
public class EcSalesOrderController {

    private final EcSalesOrderService ecSalesOrderService;

    @GetMapping
    public ApiResult<PageResult<EcSalesOrderDetailVO>> list(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) Long shopId,
                                                            @RequestParam(required = false) String orderTimeFrom,
                                                            @RequestParam(required = false) String orderTimeTo,
                                                            @RequestParam(required = false) Long page,
                                                            @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecSalesOrderService.pageOrders(
                keyword, status, shopId, orderTimeFrom, orderTimeTo, page, pageSize));
    }

    @GetMapping("/monthly-overview")
    public ApiResult<EcSalesOrderMonthlyOverviewVO> monthlyOverview(
            @RequestParam String orderMonth,
            @RequestParam(required = false) Long shopId) {
        return ApiResult.ok(ecSalesOrderService.getMonthlyOverview(orderMonth, shopId));
    }

    @GetMapping("/{id}")
    public ApiResult<EcSalesOrderDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecSalesOrderService.getOrderDetail(id));
    }

    @PostMapping
    public ApiResult<EcSalesOrderDetailVO> create(@RequestBody EcSalesOrderSaveRequest request) {
        return ApiResult.ok(ecSalesOrderService.createOrder(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcSalesOrderDetailVO> update(@PathVariable Long id,
                                                    @RequestBody EcSalesOrderSaveRequest request) {
        return ApiResult.ok(ecSalesOrderService.updateOrder(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResult<EcSalesOrderDetailVO> confirm(@PathVariable Long id) {
        return ApiResult.ok(ecSalesOrderService.confirmOrder(id));
    }

    @PostMapping("/{id}/ship")
    public ApiResult<EcSalesOrderDetailVO> shipOrder(@PathVariable Long id) {
        return ApiResult.ok(ecSalesOrderService.shipOrder(id));
    }

    @PostMapping("/{id}/lines/{lineId}/ship")
    public ApiResult<EcSalesOrderDetailVO> shipLine(@PathVariable Long id, @PathVariable Long lineId) {
        return ApiResult.ok(ecSalesOrderService.shipLine(id, lineId));
    }

    @PostMapping("/{id}/lines/{lineId}/refund")
    public ApiResult<EcSalesOrderDetailVO> refundLine(@PathVariable Long id,
                                                      @PathVariable Long lineId,
                                                      @RequestBody EcSalesOrderLineRefundRequest request) {
        return ApiResult.ok(ecSalesOrderService.refundLine(id, lineId, request));
    }

    @PostMapping("/{id}/lines/{lineId}/cancel")
    public ApiResult<EcSalesOrderDetailVO> cancelLine(@PathVariable Long id, @PathVariable Long lineId) {
        return ApiResult.ok(ecSalesOrderService.cancelLine(id, lineId));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecSalesOrderService.deleteOrder(id);
        return ApiResult.ok();
    }

    @GetMapping("/import/{batchId}")
    public ApiResult<EcSalesOrderImportPreviewVO> getImportPreview(@PathVariable Long batchId) {
        return ApiResult.ok(ecSalesOrderService.getImportPreview(batchId));
    }

    @PostMapping("/import/preview")
    public ApiResult<EcSalesOrderImportPreviewVO> previewImport(@RequestBody EcSalesOrderImportPreviewRequest request) {
        return ApiResult.ok(ecSalesOrderService.previewImport(request));
    }

    @PostMapping("/import/upload")
    public ApiResult<EcSalesOrderImportPreviewVO> uploadImport(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam Long shopId,
                                                                 @RequestParam(required = false) Long profileId,
                                                                 @RequestParam(required = false) String orderMonth) {
        return ApiResult.ok(ecSalesOrderService.uploadImport(file, profileId, shopId, orderMonth));
    }

    @PostMapping("/import/{batchId}/manual-costs")
    public ApiResult<EcSalesOrderImportPreviewVO> updateImportManualCosts(
            @PathVariable Long batchId,
            @RequestBody EcSalesOrderImportManualCostUpdateRequest request) {
        return ApiResult.ok(ecSalesOrderService.updateImportManualCosts(batchId, request));
    }

    @PostMapping("/import/{batchId}/commit")
    public ApiResult<EcSalesOrderImportPreviewVO> commitImport(
            @PathVariable Long batchId,
            @RequestBody(required = false) EcSalesOrderImportManualCostUpdateRequest request) {
        return ApiResult.ok(ecSalesOrderService.commitImport(batchId, request));
    }

    @PostMapping("/import/{batchId}/reparse")
    public ApiResult<EcSalesOrderImportPreviewVO> reparseImport(@PathVariable Long batchId) {
        return ApiResult.ok(ecSalesOrderService.reparseImport(batchId));
    }

    @PostMapping("/import/{batchId}/replace-file")
    public ApiResult<EcSalesOrderImportPreviewVO> replaceImportFile(@PathVariable Long batchId,
                                                                      @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(ecSalesOrderService.replaceImportFile(batchId, file));
    }
}
