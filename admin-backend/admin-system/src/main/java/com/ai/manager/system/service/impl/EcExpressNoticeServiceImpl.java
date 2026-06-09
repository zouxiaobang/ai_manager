package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcExpressNoticeSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressNotice;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.vo.EcExpressNoticeVO;
import com.ai.manager.system.mapper.EcExpressNoticeMapper;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.ai.manager.system.service.EcExpressNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EcExpressNoticeServiceImpl extends ServiceImpl<EcExpressNoticeMapper, EcExpressNotice>
        implements EcExpressNoticeService {

    private final EcExpressStationMapper ecExpressStationMapper;

    @Override
    public List<EcExpressNoticeVO> listNotices(Long stationId) {
        requireStation(stationId);
        List<EcExpressNotice> notices = list(new LambdaQueryWrapper<EcExpressNotice>()
                .eq(EcExpressNotice::getStationId, stationId)
                .orderByAsc(EcExpressNotice::getSortOrder)
                .orderByAsc(EcExpressNotice::getId));
        List<EcExpressNoticeVO> result = new ArrayList<>();
        for (EcExpressNotice notice : notices) {
            result.add(toVO(notice));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcExpressNoticeVO createNotice(EcExpressNoticeSaveRequest request) {
        validateRequest(request, true);
        requireStation(request.getStationId());
        EcExpressNotice notice = applyFields(request, new EcExpressNotice());
        save(notice);
        return toVO(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcExpressNoticeVO updateNotice(Long id, EcExpressNoticeSaveRequest request) {
        EcExpressNotice existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateRequest(request, false);
        if (request.getStationId() != null && !request.getStationId().equals(existing.getStationId())) {
            requireStation(request.getStationId());
            existing.setStationId(request.getStationId());
        }
        applyFields(request, existing);
        updateById(existing);
        return toVO(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        EcExpressNotice existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        removeById(id);
    }

    private void validateRequest(EcExpressNoticeSaveRequest request, boolean creating) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        if (creating && request.getStationId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "站点不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "须知内容不能为空");
        }
    }

    private EcExpressNotice applyFields(EcExpressNoticeSaveRequest request, EcExpressNotice notice) {
        if (request.getStationId() != null) {
            notice.setStationId(request.getStationId());
        }
        notice.setContent(request.getContent().trim());
        notice.setHighlightRed(Boolean.TRUE.equals(request.getHighlightRed()) ? 1 : 0);
        notice.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        return notice;
    }

    private void requireStation(Long stationId) {
        EcExpressStation station = ecExpressStationMapper.selectById(stationId);
        if (station == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "快递站点不存在");
        }
    }

    private EcExpressNoticeVO toVO(EcExpressNotice notice) {
        EcExpressNoticeVO vo = new EcExpressNoticeVO();
        vo.setId(notice.getId());
        vo.setStationId(notice.getStationId());
        vo.setContent(notice.getContent());
        vo.setHighlightRed(notice.getHighlightRed() != null && notice.getHighlightRed() == 1);
        vo.setSortOrder(notice.getSortOrder());
        vo.setUpdateTime(notice.getUpdateTime());
        return vo;
    }
}
