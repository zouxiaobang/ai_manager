package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.SysImportProfile;
import com.ai.manager.system.domain.vo.SysImportFieldVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一导入列映射：JSON 读写、校验、默认值、作用域键。
 */
@Component
@RequiredArgsConstructor
public class SysImportColumnMappingSupport {

    private final ObjectMapper objectMapper;

    public static String expressStationScopeKey(Long expressStationId) {
        if (expressStationId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "快递站点 ID 不能为空");
        }
        return "express_station:" + expressStationId;
    }

    public static String platformScopeKey(Long platformId) {
        if (platformId == null) {
            return null;
        }
        return "platform:" + platformId;
    }

    public Map<String, String> readColumnMapping(SysImportProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.getBizType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入配置无效");
        }
        return readColumnMapping(profile.getColumnMapping(), profile.getBizType(), true);
    }

    public Map<String, String> readColumnMapping(String json, String bizType) {
        return readColumnMapping(json, bizType, false);
    }

    public Map<String, String> readColumnMapping(String json, String bizType, boolean required) {
        if (!StringUtils.hasText(json)) {
            if (required) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入配置缺少列映射");
            }
            return defaultColumnMapping(bizType);
        }
        try {
            Map<String, String> mapping = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
            return SysImportFieldRegistry.sanitizeColumnMapping(bizType, mapping);
        } catch (Exception ex) {
            if (required) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "列映射 JSON 无效");
            }
            return defaultColumnMapping(bizType);
        }
    }

    public String writeColumnMapping(String bizType, Map<String, String> columnMapping) {
        return toJson(SysImportFieldRegistry.sanitizeColumnMapping(bizType, columnMapping));
    }

    public Map<String, String> defaultColumnMapping(String bizType) {
        LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
        for (SysImportFieldVO field : SysImportFieldRegistry.listFields(bizType)) {
            defaults.put(field.getKey(), suggestDefaultDocColumn(bizType, field.getKey()));
        }
        return SysImportFieldRegistry.sanitizeColumnMapping(bizType, defaults);
    }

    public void validateRequiredFields(String bizType, Map<String, String> columnMapping) {
        if (columnMapping == null || columnMapping.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请配置列对应关系");
        }
        for (SysImportFieldVO field : SysImportFieldRegistry.listFields(bizType)) {
            if (field.isRequired()) {
                String docCol = columnMapping.get(field.getKey());
                if (!StringUtils.hasText(docCol)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                            "必填字段未映射: " + field.getLabelZh());
                }
            }
        }
    }

    public Map<String, String> readStringMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    public Map<String, Object> readObjectMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "JSON 序列化失败");
        }
    }

    private String suggestDefaultDocColumn(String bizType, String fieldKey) {
        if (SysImportFieldRegistry.BIZ_SETTLEMENT_EXPRESS_BILL.equals(bizType)) {
            return switch (fieldKey) {
                case "tracking_number" -> "运单号";
                case "freight_amount" -> "运费";
                case "settlement_destination" -> "结算目的地";
                case "weight" -> "重量";
                case "ship_time" -> "发货时间";
                default -> "";
            };
        }
        return "";
    }
}
