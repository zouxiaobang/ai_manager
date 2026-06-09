package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.SysImportProfile;
import com.ai.manager.system.mapper.SysImportProfileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 快递站点导入名称别名：按站点存于 sys_import_profile（biz_type=EXPRESS_STATION_NAME）。
 */
@Component
@RequiredArgsConstructor
public class ExpressStationNameAliasSupport {

    private static final String EMPTY_COLUMN_MAPPING = "{}";

    private final SysImportProfileMapper sysImportProfileMapper;
    private final SysImportColumnMappingSupport columnMappingSupport;

    private volatile Map<String, Long> aliasCache;

    public List<String> listAliases(Long stationId) {
        if (stationId == null) {
            return List.of();
        }
        SysImportProfile profile = findProfile(stationId);
        if (profile == null) {
            return List.of();
        }
        Map<String, String> valueMapping = columnMappingSupport.readStringMap(profile.getValueMapping());
        return new ArrayList<>(valueMapping.keySet());
    }

    public void saveAliases(Long stationId, String canonicalName, List<String> aliases) {
        if (stationId == null) {
            return;
        }
        List<String> normalized = normalizeAliases(canonicalName, aliases);
        validateNoCrossStationConflict(stationId, normalized);

        LinkedHashMap<String, String> valueMapping = new LinkedHashMap<>();
        String stationIdText = String.valueOf(stationId);
        for (String alias : normalized) {
            valueMapping.put(alias, stationIdText);
        }

        SysImportProfile entity = findProfile(stationId);
        if (valueMapping.isEmpty()) {
            if (entity != null) {
                sysImportProfileMapper.deleteById(entity.getId());
                invalidateCache();
            }
            return;
        }
        if (entity == null) {
            entity = new SysImportProfile();
            entity.setEnabled(1);
        }
        entity.setName(buildProfileName(canonicalName));
        entity.setBizType(SysImportFieldRegistry.BIZ_EXPRESS_STATION_NAME);
        entity.setPlatformId(null);
        entity.setScopeKey(SysImportColumnMappingSupport.expressStationScopeKey(stationId));
        entity.setShopId(null);
        entity.setFileType("XLSX");
        entity.setHeaderRow(1);
        entity.setDataStartRow(2);
        entity.setColumnMapping(EMPTY_COLUMN_MAPPING);
        entity.setValueMapping(columnMappingSupport.toJson(valueMapping));
        entity.setExtraConfig(null);

        try {
            if (entity.getId() != null) {
                sysImportProfileMapper.updateById(entity);
            } else {
                sysImportProfileMapper.insert(entity);
            }
        } catch (DuplicateKeyException ex) {
            SysImportProfile existing = findProfile(stationId);
            if (existing == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "站点名称别名配置保存失败");
            }
            entity.setId(existing.getId());
            sysImportProfileMapper.updateById(entity);
        }
        invalidateCache();
    }

    public void deleteProfile(Long stationId) {
        if (stationId == null) {
            return;
        }
        SysImportProfile profile = findProfile(stationId);
        if (profile != null) {
            sysImportProfileMapper.deleteById(profile.getId());
            invalidateCache();
        }
    }

    public Long resolveStationId(String importName) {
        if (!StringUtils.hasText(importName)) {
            return null;
        }
        String trimmed = importName.trim();
        Map<String, Long> aliasMap = loadAliasMap();
        Long exact = aliasMap.get(trimmed);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Long> entry : aliasMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(trimmed)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void validateNoCrossStationConflict(Long stationId, List<String> aliases) {
        if (aliases.isEmpty()) {
            return;
        }
        Map<String, Long> aliasMap = loadAliasMap();
        for (String alias : aliases) {
            Long owner = aliasMap.get(alias);
            if (owner == null) {
                for (Map.Entry<String, Long> entry : aliasMap.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(alias)) {
                        owner = entry.getValue();
                        break;
                    }
                }
            }
            if (owner != null && !owner.equals(stationId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "导入名称「" + alias + "」已被其他快递站点使用");
            }
        }
    }

    private List<String> normalizeAliases(String canonicalName, List<String> aliases) {
        Set<String> result = new LinkedHashSet<>();
        String canonical = StringUtils.hasText(canonicalName) ? canonicalName.trim() : null;
        if (aliases != null) {
            for (String alias : aliases) {
                if (!StringUtils.hasText(alias)) {
                    continue;
                }
                String trimmed = alias.trim();
                if (canonical != null && canonical.equals(trimmed)) {
                    continue;
                }
                result.add(trimmed);
            }
        }
        return new ArrayList<>(result);
    }

    private Map<String, Long> loadAliasMap() {
        Map<String, Long> cached = aliasCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (aliasCache != null) {
                return aliasCache;
            }
            List<SysImportProfile> profiles = sysImportProfileMapper.selectList(new LambdaQueryWrapper<SysImportProfile>()
                    .eq(SysImportProfile::getBizType, SysImportFieldRegistry.BIZ_EXPRESS_STATION_NAME)
                    .eq(SysImportProfile::getEnabled, 1));
            LinkedHashMap<String, Long> map = new LinkedHashMap<>();
            for (SysImportProfile profile : profiles) {
                Long stationId = parseStationId(profile.getScopeKey());
                if (stationId == null) {
                    continue;
                }
                Map<String, String> valueMapping = columnMappingSupport.readStringMap(profile.getValueMapping());
                for (String alias : valueMapping.keySet()) {
                    if (StringUtils.hasText(alias)) {
                        map.put(alias.trim(), stationId);
                    }
                }
            }
            aliasCache = Map.copyOf(map);
            return aliasCache;
        }
    }

    private SysImportProfile findProfile(Long stationId) {
        return sysImportProfileMapper.selectOne(new LambdaQueryWrapper<SysImportProfile>()
                .eq(SysImportProfile::getBizType, SysImportFieldRegistry.BIZ_EXPRESS_STATION_NAME)
                .eq(SysImportProfile::getScopeKey, SysImportColumnMappingSupport.expressStationScopeKey(stationId))
                .orderByDesc(SysImportProfile::getUpdateTime)
                .last("LIMIT 1"));
    }

    private Long parseStationId(String scopeKey) {
        if (!StringUtils.hasText(scopeKey) || !scopeKey.startsWith("express_station:")) {
            return null;
        }
        try {
            return Long.parseLong(scopeKey.substring("express_station:".length()).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildProfileName(String canonicalName) {
        if (StringUtils.hasText(canonicalName)) {
            return canonicalName.trim() + "导入名称";
        }
        return "快递站点导入名称";
    }

    private void invalidateCache() {
        aliasCache = null;
    }
}
