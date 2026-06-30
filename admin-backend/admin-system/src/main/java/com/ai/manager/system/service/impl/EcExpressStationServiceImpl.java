package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcExpressNoticeSaveRequest;
import com.ai.manager.system.domain.dto.EcExpressPriceSaveRequest;
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
import com.ai.manager.system.service.EcEcommerceImageRenameService;
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
    private final EcEcommerceImageRenameService ecEcommerceImageRenameService;

    @Override
    public PageResult<EcExpressStationDetailVO> pageStations(String keyword, Boolean defaultOnly, List<String> regionNames,
                                                             Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcExpressStation> wrapper = new LambdaQueryWrapper<EcExpressStation>()
                .orderByDesc(EcExpressStation::getIsDefault)
                .orderByDesc(EcExpressStation::getId);
        if (Boolean.TRUE.equals(defaultOnly)) {
            wrapper.eq(EcExpressStation::getIsDefault, 1);
        }
        if (regionNames != null && !regionNames.isEmpty()) {
            List<Long> stationIds = ecExpressPriceMapper.selectList(new LambdaQueryWrapper<EcExpressPrice>()
                            .select(EcExpressPrice::getStationId)
                            .in(EcExpressPrice::getProvinceName, regionNames))
                    .stream()
                    .map(EcExpressPrice::getStationId)
                    .distinct()
                    .toList();
            if (stationIds.isEmpty()) {
                return PageUtils.of(List.of(), 0L, p, ps);
            }
            wrapper.in(EcExpressStation::getId, stationIds);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcExpressStation::getName, kw)
                    .or().like(EcExpressStation::getContact, kw)
                    .or().like(EcExpressStation::getAddress, kw));
        }
        Page<EcExpressStation> entityPage = page(new Page<>(p, ps), wrapper);
        List<EcExpressStationDetailVO> records = new ArrayList<>();
        for (EcExpressStation station : entityPage.getRecords()) {
            EcExpressStationDetailVO vo = toDetailVO(station, List.of(), List.of(),
                    expressStationNameAliasSupport.listAliases(station.getId()));
            long priceCount = ecExpressPriceMapper.selectCount(new LambdaQueryWrapper<EcExpressPrice>()
                    .eq(EcExpressPrice::getStationId, station.getId()));
            long noticeCount = ecExpressNoticeMapper.selectCount(new LambdaQueryWrapper<EcExpressNotice>()
                    .eq(EcExpressNotice::getStationId, station.getId()));
            vo.setPriceCount((int) priceCount);
            vo.setNoticeCount((int) noticeCount);
            records.add(vo);
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
    public EcExpressStationDetailVO copyStation(Long id) {
        EcExpressStationDetailVO source = getStationDetail(id);
        EcExpressStationSaveRequest request = new EcExpressStationSaveRequest();
        request.setName(source.getName() + " (复制)");
        request.setAvatarUrl(source.getAvatarUrl());
        request.setContact(source.getContact());
        request.setAddress(source.getAddress());
        request.setLabelPrice(source.getLabelPrice());
        request.setIsDefault(false);
        // 导入别名全局唯一，复制站点时不复制，需在新站点上单独配置
        request.setNameAliases(List.of());

        EcExpressStationDetailVO created = createStation(request);
        Long newStationId = created.getId();

        if (source.getPrices() != null) {
            for (EcExpressPriceVO price : source.getPrices()) {
                ecExpressPriceService.createPrice(toPriceSaveRequest(newStationId, price));
            }
        }
        if (source.getNotices() != null) {
            for (EcExpressNoticeVO notice : source.getNotices()) {
                ecExpressNoticeService.createNotice(toNoticeSaveRequest(newStationId, notice));
            }
        }
        return getStationDetail(newStationId);
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
        station.setAvatarUrl(ecEcommerceImageRenameService.normalizeExpressAvatar(
                request.getAvatarUrl(), request.getName().trim()));
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
        vo.setAvatarUrl(station.getAvatarUrl());
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

    private EcExpressPriceSaveRequest toPriceSaveRequest(Long stationId, EcExpressPriceVO price) {
        EcExpressPriceSaveRequest request = new EcExpressPriceSaveRequest();
        request.setStationId(stationId);
        request.setProvinceName(price.getProvinceName());
        request.setPriceW03Kg(price.getPriceW03Kg());
        request.setPriceW05Kg(price.getPriceW05Kg());
        request.setPriceW1Kg(price.getPriceW1Kg());
        request.setPriceW15Kg(price.getPriceW15Kg());
        request.setPriceW2Kg(price.getPriceW2Kg());
        request.setPriceW25Kg(price.getPriceW25Kg());
        request.setPriceW3Kg(price.getPriceW3Kg());
        request.setOver3FirstPrice(price.getOver3FirstPrice());
        request.setOver3AdditionalPrice(price.getOver3AdditionalPrice());
        return request;
    }

    private EcExpressNoticeSaveRequest toNoticeSaveRequest(Long stationId, EcExpressNoticeVO notice) {
        EcExpressNoticeSaveRequest request = new EcExpressNoticeSaveRequest();
        request.setStationId(stationId);
        request.setContent(notice.getContent());
        request.setHighlightRed(notice.getHighlightRed());
        request.setSortOrder(notice.getSortOrder());
        return request;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
