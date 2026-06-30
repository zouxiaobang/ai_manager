package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.SysImportProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ai.manager.system.service.support.EcSettingsBuiltinSupport;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 导入时将平台原始订单/子订单状态文案映射为系统行状态（PAID、SHIPPED、COMPLETED 等）。
 */
public final class EcImportStatusSupport {

    private static final String DEFAULT_LINE_STATUS = "PAID";

    private static final Map<String, String> BUILTIN_PLATFORM_STATUS = new LinkedHashMap<>();

    static {
        BUILTIN_PLATFORM_STATUS.put("交易成功", "COMPLETED");
        BUILTIN_PLATFORM_STATUS.put("交易关闭", "CANCELLED");
        BUILTIN_PLATFORM_STATUS.put("确认收货", "COMPLETED");
        BUILTIN_PLATFORM_STATUS.put("卖家已发货，等待买家确认", "SHIPPED");
        BUILTIN_PLATFORM_STATUS.put("等待买家确认收货", "SHIPPED");
        BUILTIN_PLATFORM_STATUS.put("卖家已发货", "SHIPPED");
        BUILTIN_PLATFORM_STATUS.put("等待买家确认", "SHIPPED");
        BUILTIN_PLATFORM_STATUS.put("买家已付款，等待卖家发货", "PAID");
        BUILTIN_PLATFORM_STATUS.put("买家已付款", "PAID");
        BUILTIN_PLATFORM_STATUS.put("等待卖家发货", "PAID");
        BUILTIN_PLATFORM_STATUS.put("待发货", "PAID");
        BUILTIN_PLATFORM_STATUS.put("已关闭", "CANCELLED");
        BUILTIN_PLATFORM_STATUS.put("已发货", "SHIPPED");
        BUILTIN_PLATFORM_STATUS.put("已完成", "COMPLETED");
        BUILTIN_PLATFORM_STATUS.put("已退款", "REFUNDED");
        BUILTIN_PLATFORM_STATUS.put("退款成功", "REFUNDED");
        BUILTIN_PLATFORM_STATUS.put("部分退款", "PARTIAL_REFUND");
        BUILTIN_PLATFORM_STATUS.put("退款中", "REFUNDED");
        BUILTIN_PLATFORM_STATUS.put("退货退款", "RETURNED");
        BUILTIN_PLATFORM_STATUS.put("已取消", "CANCELLED");
    }

    private final Map<String, String> valueMapping;
    private final String defaultLineStatus;

    private EcImportStatusSupport(Map<String, String> valueMapping, String defaultLineStatus) {
        this.valueMapping = valueMapping != null ? valueMapping : Map.of();
        this.defaultLineStatus = normalizeStatus(defaultLineStatus);
    }

    public static EcImportStatusSupport from(SysImportProfile profile, ObjectMapper objectMapper) {
        return from(profile, objectMapper, null, null);
    }

    public static EcImportStatusSupport from(SysImportProfile profile, ObjectMapper objectMapper,
                                             Map<String, String> systemStatusMapping,
                                             String systemDefaultLineStatus) {
        Map<String, String> mapping = new LinkedHashMap<>();
        if (systemStatusMapping != null && !systemStatusMapping.isEmpty()) {
            mapping.putAll(systemStatusMapping);
        } else {
            mapping.putAll(EcSettingsBuiltinSupport.defaultOrderImportStatusMapping());
        }
        String defaultStatus = StringUtils.hasText(systemDefaultLineStatus)
                ? systemDefaultLineStatus.trim()
                : DEFAULT_LINE_STATUS;
        if (profile == null) {
            return new EcImportStatusSupport(mapping, defaultStatus);
        }
        if (StringUtils.hasText(profile.getValueMapping()) && objectMapper != null) {
            try {
                Map<String, String> custom = objectMapper.readValue(profile.getValueMapping(),
                        new TypeReference<Map<String, String>>() {
                        });
                if (custom != null) {
                    mapping.putAll(custom);
                }
            } catch (Exception ignored) {
                /* use builtin only */
            }
        }
        if (StringUtils.hasText(profile.getExtraConfig()) && objectMapper != null) {
            try {
                Map<String, Object> extra = objectMapper.readValue(profile.getExtraConfig(),
                        new TypeReference<Map<String, Object>>() {
                        });
                Object ds = extra != null ? extra.get("defaultLineStatus") : null;
                if (ds != null && StringUtils.hasText(String.valueOf(ds))) {
                    defaultStatus = String.valueOf(ds).trim();
                }
            } catch (Exception ignored) {
                /* keep default */
            }
        }
        return new EcImportStatusSupport(mapping, defaultStatus);
    }

    /**
     * @return 系统行状态：PAID、SHIPPED、COMPLETED、CANCELLED、PARTIAL_REFUND、REFUNDED、RETURNED
     */
    public String resolveLineStatus(String platformStatus) {
        return resolveDetailed(platformStatus).lineStatus();
    }

    /**
     * 解析平台状态；有文案但未命中映射时 {@code matched=false}，需人工指定行状态。
     */
    public ResolveResult resolveDetailed(String platformStatus) {
        if (!StringUtils.hasText(platformStatus)) {
            return new ResolveResult(false, null, null);
        }
        String text = platformStatus.trim();
        String exact = valueMapping.get(text);
        if (exact != null) {
            return new ResolveResult(true, normalizeStatus(exact), text);
        }
        String best = null;
        int bestLen = 0;
        for (Map.Entry<String, String> entry : valueMapping.entrySet()) {
            String key = entry.getKey();
            if (!StringUtils.hasText(key) || key.length() <= bestLen) {
                continue;
            }
            if (text.contains(key)) {
                best = entry.getValue();
                bestLen = key.length();
            }
        }
        if (best != null) {
            return new ResolveResult(true, normalizeStatus(best), text);
        }
        return new ResolveResult(false, null, text);
    }

    public record ResolveResult(boolean matched, String lineStatus, String platformText) {
    }

    private static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return DEFAULT_LINE_STATUS;
        }
        return status.trim().toUpperCase();
    }
}
