package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcExpressStationSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.vo.EcExpressStationDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 电商快递站点服务接口
 *
 * <p>提供电商快递站点的分页查询、详情查看、创建、更新、复制和删除等基础CRUD功能。</p>
 */
public interface EcExpressStationService extends IService<EcExpressStation> {

    /**
     * 分页查询快递站点列表
     *
     * @param keyword     关键词（站点名称等）
     * @param defaultOnly 是否仅显示默认站点
     * @param regionNames 区域名称列表
     * @param page        页码
     * @param pageSize    每页条数
     * @return 快递站点分页结果
     */
    PageResult<EcExpressStationDetailVO> pageStations(String keyword, Boolean defaultOnly, List<String> regionNames,
                                                      Long page, Long pageSize);

    /**
     * 获取快递站点详情
     *
     * @param id 站点ID
     * @return 站点详情信息
     */
    EcExpressStationDetailVO getStationDetail(Long id);

    /**
     * 创建快递站点
     *
     * @param request 站点保存请求参数
     * @return 创建后的站点详情
     */
    EcExpressStationDetailVO createStation(EcExpressStationSaveRequest request);

    /**
     * 更新快递站点
     *
     * @param id      站点ID
     * @param request 站点保存请求参数
     * @return 更新后的站点详情
     */
    EcExpressStationDetailVO updateStation(Long id, EcExpressStationSaveRequest request);

    /**
     * 复制快递站点
     *
     * @param id 站点ID
     * @return 复制后的站点详情
     */
    EcExpressStationDetailVO copyStation(Long id);

    /**
     * 删除快递站点
     *
     * @param id 站点ID
     */
    void deleteStation(Long id);
}
