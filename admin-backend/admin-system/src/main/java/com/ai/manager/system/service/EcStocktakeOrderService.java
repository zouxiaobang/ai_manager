package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcStocktakeOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcStocktakeOrder;
import com.ai.manager.system.domain.vo.EcStocktakeOrderDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface EcStocktakeOrderService extends IService<EcStocktakeOrder> {

    PageResult<EcStocktakeOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                    Long page, Long pageSize);

    EcStocktakeOrderDetailVO getOrderDetail(Long id);

    EcStocktakeOrderDetailVO createOrder(EcStocktakeOrderSaveRequest request);

    EcStocktakeOrderDetailVO updateOrder(Long id, EcStocktakeOrderSaveRequest request);

    EcStocktakeOrderDetailVO confirmOrder(Long id);

    void cancelOrder(Long id);

    void deleteOrder(Long id);
}
