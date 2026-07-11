package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcInboundOrderConfirmRequest;
import com.ai.manager.system.domain.dto.EcInboundOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcInboundOrder;
import com.ai.manager.system.domain.vo.EcInboundOrderDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 电商入库单服务接口
 *
 * <p>提供电商入库单的分页查询、详情查看、创建、更新、确认、取消和删除等入库单管理功能。</p>
 */
public interface EcInboundOrderService extends IService<EcInboundOrder> {

    /**
     * 分页查询入库单列表
     *
     * @param keyword    关键词（入库单号等）
     * @param status     入库单状态
     * @param factoryId  工厂ID
     * @param orderMonth 入库月份
     * @param page       页码
     * @param pageSize   每页条数
     * @return 入库单分页结果
     */
    PageResult<EcInboundOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                   String orderMonth, Long page, Long pageSize);

    /**
     * 获取入库单详情
     *
     * @param id 入库单ID
     * @return 入库单详情信息
     */
    EcInboundOrderDetailVO getOrderDetail(Long id);

    /**
     * 创建入库单
     *
     * @param request 入库单保存请求参数
     * @return 创建后的入库单详情
     */
    EcInboundOrderDetailVO createOrder(EcInboundOrderSaveRequest request);

    /**
     * 更新入库单
     *
     * @param id      入库单ID
     * @param request 入库单保存请求参数
     * @return 更新后的入库单详情
     */
    EcInboundOrderDetailVO updateOrder(Long id, EcInboundOrderSaveRequest request);

    /**
     * 确认入库单
     *
     * @param id      入库单ID
     * @param request 入库单确认请求参数
     * @return 确认后的入库单详情
     */
    EcInboundOrderDetailVO confirmOrder(Long id, EcInboundOrderConfirmRequest request);

    /**
     * 取消入库单
     *
     * @param id 入库单ID
     */
    void cancelOrder(Long id);

    /**
     * 删除入库单
     *
     * @param id 入库单ID
     */
    void deleteOrder(Long id);
}
