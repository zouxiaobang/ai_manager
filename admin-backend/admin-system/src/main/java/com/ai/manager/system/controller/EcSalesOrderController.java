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

/**
 * 电商销售订单控制器
 *
 * <p>所属模块：电商模块-销售订单管理</p>
 * <p>API路径前缀：/api/ecommerce/sales-orders</p>
 * <p>功能描述：提供销售订单的增删改查、订单状态流转、订单行操作以及订单导入等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/sales-orders")
@RequiredArgsConstructor
public class EcSalesOrderController {

    private final EcSalesOrderService ecSalesOrderService;

    /**
     * 分页查询销售订单列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/sales-orders</p>
     *
     * @param keyword 关键词，用于搜索订单号、买家信息等
     * @param status 订单状态
     * @param shopId 店铺ID
     * @param orderTimeFrom 下单时间起始
     * @param orderTimeTo 下单时间结束
     * @param page 页码
     * @param pageSize 每页条数
     * @return 销售订单分页结果
     */
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

    /**
     * 获取月度订单概览
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/sales-orders/monthly-overview</p>
     *
     * @param orderMonth 订单月份
     * @param shopId 店铺ID
     * @return 月度订单概览信息
     */
    @GetMapping("/monthly-overview")
    public ApiResult<EcSalesOrderMonthlyOverviewVO> monthlyOverview(
            @RequestParam String orderMonth,
            @RequestParam(required = false) Long shopId) {
        return ApiResult.ok(ecSalesOrderService.getMonthlyOverview(orderMonth, shopId));
    }

    /**
     * 获取销售订单详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/sales-orders/{id}</p>
     *
     * @param id 订单ID
     * @return 销售订单详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcSalesOrderDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecSalesOrderService.getOrderDetail(id));
    }

    /**
     * 创建销售订单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders</p>
     *
     * @param request 订单保存请求参数
     * @return 创建后的销售订单详情
     */
    @PostMapping
    public ApiResult<EcSalesOrderDetailVO> create(@RequestBody EcSalesOrderSaveRequest request) {
        return ApiResult.ok(ecSalesOrderService.createOrder(request));
    }

    /**
     * 更新销售订单
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/sales-orders/{id}</p>
     *
     * @param id 订单ID
     * @param request 订单保存请求参数
     * @return 更新后的销售订单详情
     */
    @PutMapping("/{id}")
    public ApiResult<EcSalesOrderDetailVO> update(@PathVariable Long id,
                                                    @RequestBody EcSalesOrderSaveRequest request) {
        return ApiResult.ok(ecSalesOrderService.updateOrder(id, request));
    }

    /**
     * 确认销售订单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/{id}/confirm</p>
     *
     * @param id 订单ID
     * @return 确认后的销售订单详情
     */
    @PostMapping("/{id}/confirm")
    public ApiResult<EcSalesOrderDetailVO> confirm(@PathVariable Long id) {
        return ApiResult.ok(ecSalesOrderService.confirmOrder(id));
    }

    /**
     * 销售订单整单发货
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/{id}/ship</p>
     *
     * @param id 订单ID
     * @return 发货后的销售订单详情
     */
    @PostMapping("/{id}/ship")
    public ApiResult<EcSalesOrderDetailVO> shipOrder(@PathVariable Long id) {
        return ApiResult.ok(ecSalesOrderService.shipOrder(id));
    }

    /**
     * 销售订单行发货
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/{id}/lines/{lineId}/ship</p>
     *
     * @param id 订单ID
     * @param lineId 订单行ID
     * @return 发货后的销售订单详情
     */
    @PostMapping("/{id}/lines/{lineId}/ship")
    public ApiResult<EcSalesOrderDetailVO> shipLine(@PathVariable Long id, @PathVariable Long lineId) {
        return ApiResult.ok(ecSalesOrderService.shipLine(id, lineId));
    }

    /**
     * 销售订单行退款
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/{id}/lines/{lineId}/refund</p>
     *
     * @param id 订单ID
     * @param lineId 订单行ID
     * @param request 退款请求参数
     * @return 退款后的销售订单详情
     */
    @PostMapping("/{id}/lines/{lineId}/refund")
    public ApiResult<EcSalesOrderDetailVO> refundLine(@PathVariable Long id,
                                                      @PathVariable Long lineId,
                                                      @RequestBody EcSalesOrderLineRefundRequest request) {
        return ApiResult.ok(ecSalesOrderService.refundLine(id, lineId, request));
    }

    /**
     * 取消销售订单行
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/{id}/lines/{lineId}/cancel</p>
     *
     * @param id 订单ID
     * @param lineId 订单行ID
     * @return 取消后的销售订单详情
     */
    @PostMapping("/{id}/lines/{lineId}/cancel")
    public ApiResult<EcSalesOrderDetailVO> cancelLine(@PathVariable Long id, @PathVariable Long lineId) {
        return ApiResult.ok(ecSalesOrderService.cancelLine(id, lineId));
    }

    /**
     * 删除销售订单
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/sales-orders/{id}</p>
     *
     * @param id 订单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecSalesOrderService.deleteOrder(id);
        return ApiResult.ok();
    }

    /**
     * 获取导入预览结果
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/sales-orders/import/{batchId}</p>
     *
     * @param batchId 导入批次ID
     * @return 导入预览结果
     */
    @GetMapping("/import/{batchId}")
    public ApiResult<EcSalesOrderImportPreviewVO> getImportPreview(@PathVariable Long batchId) {
        return ApiResult.ok(ecSalesOrderService.getImportPreview(batchId));
    }

    /**
     * 预览订单导入
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/import/preview</p>
     *
     * @param request 导入预览请求参数
     * @return 导入预览结果
     */
    @PostMapping("/import/preview")
    public ApiResult<EcSalesOrderImportPreviewVO> previewImport(@RequestBody EcSalesOrderImportPreviewRequest request) {
        return ApiResult.ok(ecSalesOrderService.previewImport(request));
    }

    /**
     * 上传订单导入文件
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/import/upload</p>
     *
     * @param file 导入文件
     * @param shopId 店铺ID
     * @param profileId 配置文件ID
     * @param orderMonth 订单月份
     * @return 导入预览结果
     */
    @PostMapping("/import/upload")
    public ApiResult<EcSalesOrderImportPreviewVO> uploadImport(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam Long shopId,
                                                                 @RequestParam(required = false) Long profileId,
                                                                 @RequestParam(required = false) String orderMonth) {
        return ApiResult.ok(ecSalesOrderService.uploadImport(file, profileId, shopId, orderMonth));
    }

    /**
     * 更新导入手工成本
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/import/{batchId}/manual-costs</p>
     *
     * @param batchId 导入批次ID
     * @param request 手工成本更新请求参数
     * @return 导入预览结果
     */
    @PostMapping("/import/{batchId}/manual-costs")
    public ApiResult<EcSalesOrderImportPreviewVO> updateImportManualCosts(
            @PathVariable Long batchId,
            @RequestBody EcSalesOrderImportManualCostUpdateRequest request) {
        return ApiResult.ok(ecSalesOrderService.updateImportManualCosts(batchId, request));
    }

    /**
     * 提交订单导入
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/import/{batchId}/commit</p>
     *
     * @param batchId 导入批次ID
     * @param request 手工成本更新请求参数（可选）
     * @return 导入预览结果
     */
    @PostMapping("/import/{batchId}/commit")
    public ApiResult<EcSalesOrderImportPreviewVO> commitImport(
            @PathVariable Long batchId,
            @RequestBody(required = false) EcSalesOrderImportManualCostUpdateRequest request) {
        return ApiResult.ok(ecSalesOrderService.commitImport(batchId, request));
    }

    /**
     * 重新解析导入文件
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/import/{batchId}/reparse</p>
     *
     * @param batchId 导入批次ID
     * @return 导入预览结果
     */
    @PostMapping("/import/{batchId}/reparse")
    public ApiResult<EcSalesOrderImportPreviewVO> reparseImport(@PathVariable Long batchId) {
        return ApiResult.ok(ecSalesOrderService.reparseImport(batchId));
    }

    /**
     * 替换导入文件
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/sales-orders/import/{batchId}/replace-file</p>
     *
     * @param batchId 导入批次ID
     * @param file 新的导入文件
     * @return 导入预览结果
     */
    @PostMapping("/import/{batchId}/replace-file")
    public ApiResult<EcSalesOrderImportPreviewVO> replaceImportFile(@PathVariable Long batchId,
                                                                      @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(ecSalesOrderService.replaceImportFile(batchId, file));
    }
}
