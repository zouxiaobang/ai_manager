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
import com.baomidou.mybatisplus.extension.service.IService;

public interface EcSalesOrderService extends IService<EcSalesOrder> {

    PageResult<EcSalesOrderDetailVO> pageOrders(String keyword, String status, Long shopId,
                                                 Long page, Long pageSize);

    EcSalesOrderDetailVO getOrderDetail(Long id);

    EcSalesOrderDetailVO createOrder(EcSalesOrderSaveRequest request);

    EcSalesOrderDetailVO updateOrder(Long id, EcSalesOrderSaveRequest request);

    EcSalesOrderDetailVO confirmOrder(Long id);

    EcSalesOrderDetailVO shipLine(Long orderId, Long lineId);

    EcSalesOrderDetailVO shipOrder(Long id);

    EcSalesOrderDetailVO refundLine(Long orderId, Long lineId, EcSalesOrderLineRefundRequest request);

    EcSalesOrderDetailVO cancelLine(Long orderId, Long lineId);

    void deleteOrder(Long id);

    EcSalesOrderImportPreviewVO previewImport(EcSalesOrderImportPreviewRequest request);

    EcSalesOrderImportPreviewVO uploadImport(MultipartFile file, Long profileId, Long shopId);

    EcSalesOrderImportPreviewVO commitImport(Long batchId,
                                             EcSalesOrderImportManualCostUpdateRequest request);

    EcSalesOrderImportPreviewVO updateImportManualCosts(Long batchId,
                                                        EcSalesOrderImportManualCostUpdateRequest request);
}
