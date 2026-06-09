package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcExpressStationSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressNotice;
import com.ai.manager.system.domain.entity.EcExpressPrice;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.vo.EcExpressNoticeVO;
import com.ai.manager.system.domain.vo.EcExpressPriceVO;
import com.ai.manager.system.domain.vo.EcExpressStationDetailVO;
import com.ai.manager.system.mapper.EcExpressNoticeMapper;
import com.ai.manager.system.mapper.EcExpressPriceMapper;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.ai.manager.system.service.EcExpressNoticeService;
import com.ai.manager.system.service.EcExpressPriceService;
import com.ai.manager.system.service.EcExpressStationService;
import com.ai.manager.system.service.support.ExpressStationNameAliasSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EcExpressStationServiceImpl extends ServiceImpl<EcExpressStationMapper, EcExpressStation>
        implements EcExpressStationService {

    private final EcExpressPriceService ecExpressPriceService;
    private final EcExpressNoticeService ecExpressNoticeService;
    private final EcExpressPriceMapper ecExpressPriceMapper;
    private final EcExpressNoticeMapper ecExpressNoticeMapper;
    private final ExpressStationNameAliasSupport expressStationNameAliasSupport;

    @Override
    public PageResult<EcExpressStationDetailVO> pageStations(String keyword, Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcExpressStation> wrapper = new LambdaQueryWrapper<EcExpressStation>()
                .orderByDesc(EcExpressStation::getIsDefault)
                .orderByDesc(EcExpressStation::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcExpressStation::getName, kw)
                    .or().like(EcExpressStation::getContact, kw)
                    .or().like(EcExpressStation::getAddress, kw));
        }
        Page<EcExpressStation> entityPage = page(new Page<>(p, ps), wrapper);
        List<EcExpressStationDetailVO> records = new ArrayList<>();
        for (EcExpressStation station : entityPage.getRecords()) {
            records.add(toDetailVO(station, List.of(), List.of(),
                    expressStationNameAliasSupport.listAliases(station.getId())));
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public EcExpressStationDetailVO getStationDetail(Long id) {
        EcExpressStation station = getById(id);
        if (station == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toDetailVO(
                station,
                ecExpressPriceService.listPrices(id),
                ecExpressNoticeService.listNotices(id),
                expressStationNameAliasSupport.listAliases(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcExpressStationDetailVO createStation(EcExpressStationSaveRequest request) {
        validateRequest(request);
        EcExpressStation station = applyFields(request, new EcExpressStation());
        save(station);
        applyDefaultFlag(station.getId(), Boolean.TRUE.equals(request.getIsDefault()));
        expressStationNameAliasSupport.saveAliases(station.getId(), station.getName(), request.getNameAliases());
        return getStationDetail(station.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcExpressStationDetailVO updateStation(Long id, EcExpressStationSaveRequest request) {
        EcExpressStation existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateRequest(request);
        applyFields(request, existing);
        updateById(existing);
        applyDefaultFlag(id, Boolean.TRUE.equals(request.getIsDefault()));
        expressStationNameAliasSupport.saveAliases(id, existing.getName(), request.getNameAliases());
        return getStationDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStation(Long id) {
        EcExpressStation existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ecExpressPriceMapper.delete(new LambdaQueryWrapper<EcExpressPrice>()
                .eq(EcExpressPrice::getStationId, id));
        ecExpressNoticeMapper.delete(new LambdaQueryWrapper<EcExpressNotice>()
                .eq(EcExpressNotice::getStationId, id));
        expressStationNameAliasSupport.deleteProfile(id);
        removeById(id);
    }

    private void validateRequest(EcExpressStationSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "快递名称不能为空");
        }
    }

    private EcExpressStation applyFields(EcExpressStationSaveRequest request, EcExpressStation station) {
        station.setName(request.getName().trim());
        station.setContact(trimToNull(request.getContact()));
        station.setAddress(trimToNull(request.getAddress()));
        station.setLabelPrice(request.getLabelPrice());
        station.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()) ? 1 : 0);
        return station;
    }

    private void applyDefaultFlag(Long stationId, boolean isDefault) {
        if (!isDefault) {
            return;
        }
        update(new LambdaUpdateWrapper<EcExpressStation>()
                .set(EcExpressStation::getIsDefault, 0)
                .ne(EcExpressStation::getId, stationId));
        EcExpressStation current = getById(stationId);
        if (current != null) {
            current.setIsDefault(1);
            updateById(current);
        }
    }

    private EcExpressStationDetailVO toDetailVO(EcExpressStation station,
                                                  List<EcExpressPriceVO> prices,
                                                  List<EcExpressNoticeVO> notices,
                                                  List<String> nameAliases) {
        EcExpressStationDetailVO vo = new EcExpressStationDetailVO();
        vo.setId(station.getId());
        vo.setName(station.getName());
        vo.setNameAliases(nameAliases != null ? nameAliases : List.of());
        vo.setContact(station.getContact());
        vo.setAddress(station.getAddress());
        vo.setLabelPrice(station.getLabelPrice());
        vo.setIsDefault(station.getIsDefault() != null && station.getIsDefault() == 1);
        vo.setUpdateTime(station.getUpdateTime());
        vo.setPrices(prices);
        vo.setNotices(notices);
        return vo;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
