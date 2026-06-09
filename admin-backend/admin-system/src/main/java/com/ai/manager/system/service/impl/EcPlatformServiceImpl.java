package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcPlatformSaveRequest;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.enums.EcChannelType;
import com.ai.manager.system.domain.enums.EcPlatformCode;
import com.ai.manager.system.domain.vo.EcPlatformListItemVO;
import com.ai.manager.system.mapper.EcPlatformMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.service.EcPlatformService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EcPlatformServiceImpl extends ServiceImpl<EcPlatformMapper, EcPlatform> implements EcPlatformService {

    private final EcShopMapper ecShopMapper;

    @Override
    public PageResult<EcPlatformListItemVO> pagePlatforms(String keyword, String channelType,
                                                          Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcPlatform> wrapper = new LambdaQueryWrapper<EcPlatform>()
                .orderByDesc(EcPlatform::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcPlatform::getName, kw)
                    .or().like(EcPlatform::getNameEn, kw)
                    .or().like(EcPlatform::getRemark, kw));
        }
        if (StringUtils.hasText(channelType)) {
            wrapper.eq(EcPlatform::getChannelType, channelType.trim().toUpperCase());
        }
        Page<EcPlatform> entityPage = page(new Page<>(p, ps), wrapper);
        List<EcPlatformListItemVO> records = entityPage.getRecords().stream()
                .map(this::toListItemVO)
                .toList();
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public List<EcPlatformListItemVO> listPlatformOptions() {
        return list(new LambdaQueryWrapper<EcPlatform>()
                        .eq(EcPlatform::getStatus, "ENABLED")
                        .orderByAsc(EcPlatform::getPlatformCode)
                        .orderByAsc(EcPlatform::getId))
                .stream()
                .map(this::toListItemVO)
                .toList();
    }

    @Override
    public EcPlatformListItemVO getPlatformDetail(Long id) {
        EcPlatform platform = getById(id);
        if (platform == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toListItemVO(platform);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcPlatformListItemVO createPlatform(EcPlatformSaveRequest request) {
        validateRequest(request);
        EcPlatform platform = applySaveFields(request, new EcPlatform());
        save(platform);
        return getPlatformDetail(platform.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcPlatformListItemVO updatePlatform(Long id, EcPlatformSaveRequest request) {
        EcPlatform existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateRequest(request);
        applySaveFields(request, existing);
        updateById(existing);
        return getPlatformDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlatform(Long id) {
        EcPlatform existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long shopCount = ecShopMapper.selectCount(new LambdaQueryWrapper<EcShop>()
                .eq(EcShop::getPlatformId, id));
        if (shopCount != null && shopCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该平台下仍有店铺，无法删除");
        }
        removeById(id);
    }

    private void validateRequest(EcPlatformSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "平台名称不能为空");
        }
        if (request.getPlatformCode() == null || EcPlatformCode.fromCode(request.getPlatformCode()) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "平台标识无效");
        }
        String channelType = StringUtils.hasText(request.getChannelType())
                ? request.getChannelType().trim().toUpperCase()
                : EcChannelType.ONLINE.getCode();
        if (EcChannelType.fromCode(channelType) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "渠道模式无效");
        }
    }

    private EcPlatform applySaveFields(EcPlatformSaveRequest request, EcPlatform platform) {
        platform.setName(request.getName().trim());
        platform.setNameEn(trimToNull(request.getNameEn()));
        platform.setPlatformCode(request.getPlatformCode());
        platform.setChannelType(StringUtils.hasText(request.getChannelType())
                ? request.getChannelType().trim().toUpperCase()
                : EcChannelType.ONLINE.getCode());
        platform.setRemark(trimToNull(request.getRemark()));
        if (StringUtils.hasText(request.getStatus())) {
            platform.setStatus(request.getStatus().trim().toUpperCase());
        } else if (platform.getStatus() == null) {
            platform.setStatus("ENABLED");
        }
        return platform;
    }

    private EcPlatformListItemVO toListItemVO(EcPlatform platform) {
        EcPlatformListItemVO vo = new EcPlatformListItemVO();
        vo.setId(platform.getId());
        vo.setName(platform.getName());
        vo.setNameEn(platform.getNameEn());
        vo.setPlatformCode(platform.getPlatformCode());
        vo.setChannelType(platform.getChannelType());
        vo.setRemark(platform.getRemark());
        vo.setStatus(platform.getStatus());
        vo.setUpdateTime(platform.getUpdateTime());
        return vo;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
