package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcOutboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcOutboundOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcOutboundOrder;
import com.ai.manager.system.domain.vo.EcOutboundOrderDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface EcOutboundOrderService extends IService<EcOutboundOrder> {

    PageResult<EcOutboundOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                   Long page, Long pageSize);

    EcOutboundOrderDetailVO getOrderDetail(Long id);

    EcOutboundOrderDetailVO createOrder(EcOutboundOrderSaveRequest request);

    EcOutboundOrderDetailVO updateOrder(Long id, EcOutboundOrderSaveRequest request);

    EcOutboundOrderDetailVO confirmOrder(Long id, EcOutboundOrderConfirmRequest request);

    void cancelOrder(Long id);

    void deleteOrder(Long id);
}
