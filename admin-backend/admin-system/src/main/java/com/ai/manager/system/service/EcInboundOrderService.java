package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcInboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcInboundOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcInboundOrder;
import com.ai.manager.system.domain.vo.EcInboundOrderDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcInboundOrderService extends IService<EcInboundOrder> {

    PageResult<EcInboundOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                   Long page, Long pageSize);

    EcInboundOrderDetailVO getOrderDetail(Long id);

    EcInboundOrderDetailVO createOrder(EcInboundOrderSaveRequest request);

    EcInboundOrderDetailVO updateOrder(Long id, EcInboundOrderSaveRequest request);

    EcInboundOrderDetailVO confirmOrder(Long id, EcInboundOrderConfirmRequest request);

    void cancelOrder(Long id);

    void deleteOrder(Long id);
}
