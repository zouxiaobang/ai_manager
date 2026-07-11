package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcSalesOrderImportManualCostUpdateRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderImportPreviewRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderLineRefundRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.vo.EcSalesOrderDetailVO;
import org.springframework.web.multipart.MultipartFile;

import com.ai.manager.system.domain.vo.EcSalesOrderImportPreviewVO;
import com.ai.manager.system.domain.vo.EcSalesOrderMonthlyOverviewVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 电商销售订单服务接口
 *
 * <p>提供电商销售订单的分页查询、详情查看、创建、更新、确认、发货、退款、取消、删除等核心业务功能，
 * 同时支持订单批量导入预览、上传、提交、手动成本更新、重新解析和文件替换等导入相关操作。</p>
 */
public interface EcSalesOrderService extends IService<EcSalesOrder> {

    /**
     * 分页查询销售订单列表
     *
     * @param keyword      关键词（订单号、买家信息等）
     * @param status       订单状态
     * @param shopId       店铺ID
     * @param orderTimeFrom 下单开始时间
     * @param orderTimeTo   下单结束时间
     * @param page         页码
     * @param pageSize     每页条数
     * @return 销售订单分页结果
     */
    PageResult<EcSalesOrderDetailVO> pageOrders(String keyword, String status, Long shopId,
                                                 String orderTimeFrom, String orderTimeTo,
                                                 Long page, Long pageSize);

    /**
     * 获取月度订单概览统计
     *
     * @param orderMonth 订单月份（格式：yyyy-MM）
     * @param shopId     店铺ID
     * @return 月度订单概览数据
     */
    EcSalesOrderMonthlyOverviewVO getMonthlyOverview(String orderMonth, Long shopId);

    /**
     * 获取订单详情
     *
     * @param id 订单ID
     * @return 订单详情信息
     */
    EcSalesOrderDetailVO getOrderDetail(Long id);

    /**
     * 创建销售订单
     *
     * @param request 订单保存请求参数
     * @return 创建后的订单详情
     */
    EcSalesOrderDetailVO createOrder(EcSalesOrderSaveRequest request);

    /**
     * 更新销售订单
     *
     * @param id      订单ID
     * @param request 订单保存请求参数
     * @return 更新后的订单详情
     */
    EcSalesOrderDetailVO updateOrder(Long id, EcSalesOrderSaveRequest request);

    /**
     * 确认订单
     *
     * @param id 订单ID
     * @return 确认后的订单详情
     */
    EcSalesOrderDetailVO confirmOrder(Long id);

    /**
     * 订单行发货
     *
     * @param orderId 订单ID
     * @param lineId  订单行ID
     * @return 发货后的订单详情
     */
    EcSalesOrderDetailVO shipLine(Long orderId, Long lineId);

    /**
     * 整单发货
     *
     * @param id 订单ID
     * @return 发货后的订单详情
     */
    EcSalesOrderDetailVO shipOrder(Long id);

    /**
     * 订单行退款
     *
     * @param orderId 订单ID
     * @param lineId  订单行ID
     * @param request 退款请求参数
     * @return 退款后的订单详情
     */
    EcSalesOrderDetailVO refundLine(Long orderId, Long lineId, EcSalesOrderLineRefundRequest request);

    /**
     * 取消订单行
     *
     * @param orderId 订单ID
     * @param lineId  订单行ID
     * @return 取消后的订单详情
     */
    EcSalesOrderDetailVO cancelLine(Long orderId, Long lineId);

    /**
     * 删除订单
     *
     * @param id 订单ID
     */
    void deleteOrder(Long id);

    /**
     * 预览订单导入
     *
     * @param request 导入预览请求参数
     * @return 导入预览结果
     */
    EcSalesOrderImportPreviewVO previewImport(EcSalesOrderImportPreviewRequest request);

    /**
     * 上传订单导入文件
     *
     * @param file       导入文件
     * @param profileId  配置文件ID
     * @param shopId     店铺ID
     * @param orderMonth 订单月份
     * @return 导入预览结果
     */
    EcSalesOrderImportPreviewVO uploadImport(MultipartFile file, Long profileId, Long shopId, String orderMonth);

    /**
     * 获取导入预览结果
     *
     * @param batchId 批次ID
     * @return 导入预览结果
     */
    EcSalesOrderImportPreviewVO getImportPreview(Long batchId);

    /**
     * 提交订单导入
     *
     * @param batchId 批次ID
     * @param request 手动成本更新请求参数
     * @return 导入结果
     */
    EcSalesOrderImportPreviewVO commitImport(Long batchId,
                                             EcSalesOrderImportManualCostUpdateRequest request);

    /**
     * 更新导入手动成本
     *
     * @param batchId 批次ID
     * @param request 手动成本更新请求参数
     * @return 更新后的导入预览结果
     */
    EcSalesOrderImportPreviewVO updateImportManualCosts(Long batchId,
                                                        EcSalesOrderImportManualCostUpdateRequest request);

    /**
     * 重新解析导入文件
     *
     * @param batchId 批次ID
     * @return 重新解析后的导入预览结果
     */
    EcSalesOrderImportPreviewVO reparseImport(Long batchId);

    /**
     * 替换导入文件
     *
     * @param batchId 批次ID
     * @param file    新的导入文件
     * @return 替换后的导入预览结果
     */
    EcSalesOrderImportPreviewVO replaceImportFile(Long batchId, MultipartFile file);
}
