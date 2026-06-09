package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.SysImportProfileSaveRequest;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.entity.SysImportProfile;
import com.ai.manager.system.domain.vo.SysImportFieldVO;
import com.ai.manager.system.domain.vo.SysImportProfileVO;
import com.ai.manager.system.mapper.EcPlatformMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.mapper.SysImportProfileMapper;
import com.ai.manager.system.service.SysImportService;
import com.ai.manager.system.service.support.SysImportColumnMappingSupport;
import com.ai.manager.system.service.support.SysImportFieldRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysImportServiceImpl implements SysImportService {

    private final SysImportProfileMapper sysImportProfileMapper;
    private final EcPlatformMapper ecPlatformMapper;
    private final EcShopMapper ecShopMapper;
    private final SysImportColumnMappingSupport columnMappingSupport;

    @Override
    public List<SysImportFieldVO> listFields(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "bizType 不能为空");
        }
        return SysImportFieldRegistry.listFields(bizType.trim());
    }

    @Override
    public List<SysImportProfileVO> listProfiles(String bizType, Long platformId, Long shopId, String scopeKey) {
        Long resolvedPlatformId = resolvePlatformId(platformId, shopId);
        LambdaQueryWrapper<SysImportProfile> wrapper = new LambdaQueryWrapper<SysImportProfile>()
                .eq(SysImportProfile::getBizType, bizType)
                .eq(SysImportProfile::getEnabled, 1)
                .orderByDesc(SysImportProfile::getUpdateTime);
        if (StringUtils.hasText(scopeKey)) {
            wrapper.eq(SysImportProfile::getScopeKey, scopeKey.trim());
        } else if (resolvedPlatformId != null) {
            wrapper.eq(SysImportProfile::getPlatformId, resolvedPlatformId);
        } else if (SysImportFieldRegistry.BIZ_SALES_ORDER.equals(bizType)) {
            return List.of();
        }
        return sysImportProfileMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .sorted(Comparator.comparing(SysImportProfileVO::getPlatformId, Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(SysImportProfileVO::getName))
                .collect(Collectors.toList());
    }

    @Override
    public SysImportProfileVO getProfile(Long id) {
        SysImportProfile profile = requireProfile(id);
        return toVO(profile);
    }

    @Override
    public SysImportProfileVO getProfileByScope(String bizType, String scopeKey) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(scopeKey)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "bizType 与 scopeKey 不能为空");
        }
        SysImportProfile profile = findExistingProfileByScope(bizType.trim(), scopeKey.trim());
        if (profile == null) {
            SysImportProfileVO vo = new SysImportProfileVO();
            vo.setBizType(bizType.trim());
            vo.setScopeKey(scopeKey.trim());
            vo.setColumnMapping(SysImportFieldRegistry.isExpressStationNameBiz(bizType.trim())
                    ? Map.of() : columnMappingSupport.defaultColumnMapping(bizType.trim()));
            vo.setValueMapping(Map.of());
            vo.setHeaderRow(1);
            vo.setDataStartRow(2);
            vo.setFileType("XLSX");
            return vo;
        }
        return toVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysImportProfileVO saveProfile(SysImportProfileSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getBizType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "bizType 不能为空");
        }
        String bizType = request.getBizType().trim();
        boolean expressStationName = SysImportFieldRegistry.isExpressStationNameBiz(bizType);
        if (!expressStationName) {
            if (request.getColumnMapping() == null || request.getColumnMapping().isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请配置列对应关系");
            }
            columnMappingSupport.validateRequiredFields(bizType, request.getColumnMapping());
        }

        boolean scopeBased = SysImportFieldRegistry.isScopeBased(bizType);
        String scopeKey = trimToNull(request.getScopeKey());
        Long platformId = resolvePlatformId(request.getPlatformId(), request.getShopId());

        if (scopeBased) {
            if (!StringUtils.hasText(scopeKey)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "scopeKey 不能为空");
            }
            platformId = null;
        } else if (SysImportFieldRegistry.BIZ_SALES_ORDER.equals(bizType) && platformId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择平台");
        }
        if (platformId != null) {
            requirePlatform(platformId);
        }

        String profileName = StringUtils.hasText(request.getName())
                ? request.getName().trim()
                : (scopeBased ? "默认列映射" : null);
        if (!StringUtils.hasText(profileName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "配置名称不能为空");
        }

        SysImportProfile entity;
        if (request.getId() != null) {
            entity = requireProfile(request.getId());
        } else if (scopeBased) {
            entity = findExistingProfileByScope(bizType, scopeKey);
            if (entity == null) {
                entity = new SysImportProfile();
                entity.setEnabled(1);
            }
        } else {
            entity = findExistingProfile(bizType, platformId, profileName);
            if (entity == null) {
                entity = new SysImportProfile();
                entity.setEnabled(1);
            }
        }

        entity.setName(profileName);
        entity.setBizType(bizType);
        entity.setPlatformId(platformId);
        entity.setScopeKey(scopeBased ? scopeKey : SysImportColumnMappingSupport.platformScopeKey(platformId));
        entity.setShopId(null);
        entity.setFileType(StringUtils.hasText(request.getFileType()) ? request.getFileType().trim().toUpperCase() : "XLSX");
        entity.setHeaderRow(request.getHeaderRow() != null ? request.getHeaderRow() : 1);
        entity.setDataStartRow(request.getDataStartRow() != null ? request.getDataStartRow() : 2);
        entity.setSheetName(trimToNull(request.getSheetName()));
        if (expressStationName) {
            entity.setColumnMapping(columnMappingSupport.toJson(Map.of()));
        } else {
            entity.setColumnMapping(columnMappingSupport.writeColumnMapping(bizType, request.getColumnMapping()));
        }
        entity.setValueMapping(request.getValueMapping() != null
                ? columnMappingSupport.toJson(request.getValueMapping()) : entity.getValueMapping());
        entity.setExtraConfig(request.getExtraConfig() != null
                ? columnMappingSupport.toJson(request.getExtraConfig()) : entity.getExtraConfig());
        entity.setRemark(trimToNull(request.getRemark()));

        try {
            if (entity.getId() != null) {
                sysImportProfileMapper.updateById(entity);
            } else {
                sysImportProfileMapper.insert(entity);
            }
        } catch (DuplicateKeyException ex) {
            SysImportProfile existing = scopeBased
                    ? findExistingProfileByScope(bizType, scopeKey)
                    : findExistingProfile(bizType, platformId, profileName);
            if (existing == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已存在同名导入配置");
            }
            entity.setId(existing.getId());
            sysImportProfileMapper.updateById(entity);
        }
        return toVO(entity);
    }

    private SysImportProfile findExistingProfile(String bizType, Long platformId, String name) {
        if (platformId == null || !StringUtils.hasText(name)) {
            return null;
        }
        return sysImportProfileMapper.selectOne(new LambdaQueryWrapper<SysImportProfile>()
                .eq(SysImportProfile::getBizType, bizType)
                .eq(SysImportProfile::getPlatformId, platformId)
                .eq(SysImportProfile::getName, name)
                .last("LIMIT 1"));
    }

    private SysImportProfile findExistingProfileByScope(String bizType, String scopeKey) {
        if (!StringUtils.hasText(scopeKey)) {
            return null;
        }
        return sysImportProfileMapper.selectOne(new LambdaQueryWrapper<SysImportProfile>()
                .eq(SysImportProfile::getBizType, bizType)
                .eq(SysImportProfile::getScopeKey, scopeKey)
                .orderByDesc(SysImportProfile::getUpdateTime)
                .last("LIMIT 1"));
    }

    private Long resolvePlatformId(Long platformId, Long shopId) {
        if (platformId != null) {
            return platformId;
        }
        if (shopId == null) {
            return null;
        }
        EcShop shop = ecShopMapper.selectById(shopId);
        return shop != null ? shop.getPlatformId() : null;
    }

    private EcPlatform requirePlatform(Long platformId) {
        EcPlatform platform = ecPlatformMapper.selectById(platformId);
        if (platform == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "平台不存在");
        }
        return platform;
    }

    private SysImportProfile requireProfile(Long id) {
        SysImportProfile profile = sysImportProfileMapper.selectById(id);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return profile;
    }

    private SysImportProfileVO toVO(SysImportProfile entity) {
        SysImportProfileVO vo = new SysImportProfileVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setBizType(entity.getBizType());
        vo.setPlatformId(entity.getPlatformId());
        if (entity.getPlatformId() != null) {
            EcPlatform platform = ecPlatformMapper.selectById(entity.getPlatformId());
            if (platform != null) {
                vo.setPlatformName(platform.getName());
            }
        }
        vo.setScopeKey(entity.getScopeKey());
        vo.setShopId(entity.getShopId());
        vo.setFileType(entity.getFileType());
        vo.setHeaderRow(entity.getHeaderRow());
        vo.setDataStartRow(entity.getDataStartRow());
        vo.setSheetName(entity.getSheetName());
        vo.setColumnMapping(columnMappingSupport.readColumnMapping(entity.getColumnMapping(), entity.getBizType()));
        vo.setValueMapping(columnMappingSupport.readStringMap(entity.getValueMapping()));
        vo.setExtraConfig(columnMappingSupport.readObjectMap(entity.getExtraConfig()));
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
