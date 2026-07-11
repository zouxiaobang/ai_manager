package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcOutboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcOutboundOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcOutboundOrder;
import com.ai.manager.system.domain.vo.EcOutboundOrderDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 电商出库单服务接口
 *
 * <p>提供电商出库单的分页查询、详情查看、创建、更新、确认、取消和删除等出库单管理功能。</p>
 */
public interface EcOutboundOrderService extends IService<EcOutboundOrder> {

    /**
     * 分页查询出库单列表
     *
     * @param keyword   关键词（出库单号等）
     * @param status    出库单状态
     * @param factoryId 工厂ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 出库单分页结果
     */
    PageResult<EcOutboundOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                   Long page, Long pageSize);

    /**
     * 获取出库单详情
     *
     * @param id 出库单ID
     * @return 出库单详情信息
     */
    EcOutboundOrderDetailVO getOrderDetail(Long id);

    /**
     * 创建出库单
     *
     * @param request 出库单保存请求参数
     * @return 创建后的出库单详情
     */
    EcOutboundOrderDetailVO createOrder(EcOutboundOrderSaveRequest request);

    /**
     * 更新出库单
     *
     * @param id      出库单ID
     * @param request 出库单保存请求参数
     * @return 更新后的出库单详情
     */
    EcOutboundOrderDetailVO updateOrder(Long id, EcOutboundOrderSaveRequest request);

    /**
     * 确认出库单
     *
     * @param id      出库单ID
     * @param request 出库单确认请求参数
     * @return 确认后的出库单详情
     */
    EcOutboundOrderDetailVO confirmOrder(Long id, EcOutboundOrderConfirmRequest request);

    /**
     * 取消出库单
     *
     * @param id 出库单ID
     */
    void cancelOrder(Long id);

    /**
     * 删除出库单
     *
     * @param id 出库单ID
     */
    void deleteOrder(Long id);
}
