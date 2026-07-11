package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.EcExpressNoticeSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressNotice;
import com.ai.manager.system.domain.vo.EcExpressNoticeVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 电商快递通知服务接口
 *
 * <p>提供电商快递通知的列表查询、创建、更新和删除等快递通知管理功能。</p>
 */
public interface EcExpressNoticeService extends IService<EcExpressNotice> {

    /**
     * 查询快递通知列表
     *
     * @param stationId 站点ID
     * @return 快递通知列表
     */
    List<EcExpressNoticeVO> listNotices(Long stationId);

    /**
     * 创建快递通知
     *
     * @param request 快递通知保存请求参数
     * @return 创建后的快递通知信息
     */
    EcExpressNoticeVO createNotice(EcExpressNoticeSaveRequest request);

    /**
     * 更新快递通知
     *
     * @param id      通知ID
     * @param request 快递通知保存请求参数
     * @return 更新后的快递通知信息
     */
    EcExpressNoticeVO updateNotice(Long id, EcExpressNoticeSaveRequest request);

    /**
     * 删除快递通知
     *
     * @param id 通知ID
     */
    void deleteNotice(Long id);
}
