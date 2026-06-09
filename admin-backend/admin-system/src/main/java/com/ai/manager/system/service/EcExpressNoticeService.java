package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.EcExpressNoticeSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressNotice;
import com.ai.manager.system.domain.vo.EcExpressNoticeVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcExpressNoticeService extends IService<EcExpressNotice> {

    List<EcExpressNoticeVO> listNotices(Long stationId);

    EcExpressNoticeVO createNotice(EcExpressNoticeSaveRequest request);

    EcExpressNoticeVO updateNotice(Long id, EcExpressNoticeSaveRequest request);

    void deleteNotice(Long id);
}
