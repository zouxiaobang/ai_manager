package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.EcExpressPriceSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressPrice;
import com.ai.manager.system.domain.vo.EcExpressPriceVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 电商快递价格服务接口
 *
 * <p>提供电商快递价格的列表查询、区域名称列表、创建、更新和删除等快递价格管理功能。</p>
 */
public interface EcExpressPriceService extends IService<EcExpressPrice> {

    /**
     * 查询快递价格列表
     *
     * @param stationId 站点ID
     * @return 快递价格列表
     */
    List<EcExpressPriceVO> listPrices(Long stationId);

    /**
     * 查询区域名称列表
     *
     * @return 区域名称列表
     */
    List<String> listRegionNames();

    /**
     * 创建快递价格
     *
     * @param request 快递价格保存请求参数
     * @return 创建后的快递价格信息
     */
    EcExpressPriceVO createPrice(EcExpressPriceSaveRequest request);

    /**
     * 更新快递价格
     *
     * @param id      价格ID
     * @param request 快递价格保存请求参数
     * @return 更新后的快递价格信息
     */
    EcExpressPriceVO updatePrice(Long id, EcExpressPriceSaveRequest request);

    /**
     * 删除快递价格
     *
     * @param id 价格ID
     */
    void deletePrice(Long id);
}
