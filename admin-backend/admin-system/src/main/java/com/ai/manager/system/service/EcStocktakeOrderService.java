package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcStocktakeOrderSaveRequest;
import com.ai.manager.system.domain.entity.EcStocktakeOrder;
import com.ai.manager.system.domain.vo.EcStocktakeOrderDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 电商盘点单服务接口
 *
 * <p>提供电商盘点单的分页查询、详情查看、创建、更新、确认、取消和删除等盘点单管理功能。</p>
 */
public interface EcStocktakeOrderService extends IService<EcStocktakeOrder> {

    /**
     * 分页查询盘点单列表
     *
     * @param keyword   关键词（盘点单号等）
     * @param status    盘点单状态
     * @param factoryId 工厂ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 盘点单分页结果
     */
    PageResult<EcStocktakeOrderDetailVO> pageOrders(String keyword, String status, Long factoryId,
                                                    Long page, Long pageSize);

    /**
     * 获取盘点单详情
     *
     * @param id 盘点单ID
     * @return 盘点单详情信息
     */
    EcStocktakeOrderDetailVO getOrderDetail(Long id);

    /**
     * 创建盘点单
     *
     * @param request 盘点单保存请求参数
     * @return 创建后的盘点单详情
     */
    EcStocktakeOrderDetailVO createOrder(EcStocktakeOrderSaveRequest request);

    /**
     * 更新盘点单
     *
     * @param id      盘点单ID
     * @param request 盘点单保存请求参数
     * @return 更新后的盘点单详情
     */
    EcStocktakeOrderDetailVO updateOrder(Long id, EcStocktakeOrderSaveRequest request);

    /**
     * 确认盘点单
     *
     * @param id 盘点单ID
     * @return 确认后的盘点单详情
     */
    EcStocktakeOrderDetailVO confirmOrder(Long id);

    /**
     * 取消盘点单
     *
     * @param id 盘点单ID
     */
    void cancelOrder(Long id);

    /**
     * 删除盘点单
     *
     * @param id 盘点单ID
     */
    void deleteOrder(Long id);
}
