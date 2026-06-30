package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcCompanyInfoSaveRequest;
import com.ai.manager.system.domain.dto.EcDataRetentionSettingsSaveRequest;
import com.ai.manager.system.domain.dto.EcDeliveryNoteConfigSaveRequest;
import com.ai.manager.system.domain.dto.EcExpressSettingsSaveRequest;
import com.ai.manager.system.domain.dto.EcInventorySettingsSaveRequest;
import com.ai.manager.system.domain.dto.EcNotificationSettingsSaveRequest;
import com.ai.manager.system.domain.dto.EcOrderImportSettingsSaveRequest;
import com.ai.manager.system.domain.dto.EcOrderImportStatusSettingsSaveRequest;
import com.ai.manager.system.domain.dto.EcOutboundOrderConfigSaveRequest;
import com.ai.manager.system.domain.dto.EcRebateSettingsSaveRequest;
import com.ai.manager.system.domain.dto.EcSettlementSettingsSaveRequest;
import com.ai.manager.system.domain.entity.EcPurchaseOrderConfig;
import com.ai.manager.system.domain.entity.EcSystemConfig;
import com.ai.manager.system.domain.vo.EcCompanyInfoVO;
import com.ai.manager.system.domain.vo.EcDataRetentionSettingsVO;
import com.ai.manager.system.domain.vo.EcDeliveryNoteConfigVO;
import com.ai.manager.system.domain.vo.EcExpressSettingsVO;
import com.ai.manager.system.domain.vo.EcInventorySettingsVO;
import com.ai.manager.system.domain.vo.EcNotificationSettingsVO;
import com.ai.manager.system.domain.vo.EcOrderImportSettingsVO;
import com.ai.manager.system.domain.vo.EcOrderImportStatusSettingsVO;
import com.ai.manager.system.domain.vo.EcOutboundOrderConfigVO;
import com.ai.manager.system.domain.vo.EcPurchaseOrderConfigVO;
import com.ai.manager.system.domain.vo.EcRebateSettingsVO;
import com.ai.manager.system.domain.vo.EcSettlementSettingsVO;
import com.ai.manager.system.domain.vo.EcSettingsSummaryItemVO;
import com.ai.manager.system.mapper.EcSystemConfigMapper;
import com.ai.manager.system.service.EcPurchaseOrderConfigService;
import com.ai.manager.system.service.EcSystemSettingsService;
import com.ai.manager.system.service.support.EcSettingsBuiltinSupport;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EcSystemSettingsServiceImpl extends ServiceImpl<EcSystemConfigMapper, EcSystemConfig>
        implements EcSystemSettingsService {

    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_ORDER_IMPORT = "order_import";
    private static final String KEY_EXPRESS = "express";
    private static final String KEY_DELIVERY_NOTE = "delivery_note";
    private static final String KEY_OUTBOUND_ORDER = "outbound_order";
    private static final String KEY_ORDER_IMPORT_STATUS = "order_import_status";
    private static final String KEY_SETTLEMENT = "settlement";
    private static final String KEY_REBATE = "rebate";
    private static final String KEY_NOTIFICATION = "notification";
    private static final String KEY_DATA_RETENTION = "data_retention";
    private static final String KEY_COMPANY = "company";

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final EcPurchaseOrderConfigService ecPurchaseOrderConfigService;

    @Override
    public List<EcSettingsSummaryItemVO> listSummary() {
        List<EcSettingsSummaryItemVO> items = new ArrayList<>();

        EcPurchaseOrderConfigVO purchaseOrder = ecPurchaseOrderConfigService.getConfig();
        items.add(summaryItem("purchase-order", "采购单配置",
                StringUtils.hasText(purchaseOrder.getTitle()), purchaseOrder.getUpdateTime()));

        EcInventorySettingsVO inventory = getInventorySettings();
        items.add(summaryItem("inventory-defaults", "库存全局规则", true, inventory.getUpdateTime()));

        EcOrderImportSettingsVO orderImport = getOrderImportSettings();
        items.add(summaryItem("import-template", "订单导入默认", true, orderImport.getUpdateTime()));

        EcExpressSettingsVO express = getExpressSettings();
        items.add(summaryItem("express-bill-mapping", "快递导入默认", true, express.getUpdateTime()));

        EcDeliveryNoteConfigVO deliveryNote = getDeliveryNoteConfig();
        items.add(summaryItem("delivery-note", "送货单配置",
                StringUtils.hasText(deliveryNote.getTitle()), deliveryNote.getUpdateTime()));

        EcOutboundOrderConfigVO outboundOrder = getOutboundOrderConfig();
        items.add(summaryItem("outbound-order", "出库单配置",
                StringUtils.hasText(outboundOrder.getTitle()), outboundOrder.getUpdateTime()));

        EcOrderImportStatusSettingsVO orderImportStatus = getOrderImportStatusSettings();
        items.add(summaryItem("order-import-status", "订单状态映射",
                orderImportStatus.getStatusMapping() != null && !orderImportStatus.getStatusMapping().isEmpty(),
                orderImportStatus.getUpdateTime()));

        EcSettlementSettingsVO settlement = getSettlementSettings();
        items.add(summaryItem("profit-rules", "利润计算规则", true, settlement.getUpdateTime()));

        EcRebateSettingsVO rebate = getRebateSettings();
        items.add(summaryItem("rebate-default", "退点默认值", true, rebate.getUpdateTime()));

        EcNotificationSettingsVO notification = getNotificationSettings();
        items.add(summaryItem("notification", "通知提醒", true, notification.getUpdateTime()));

        EcDataRetentionSettingsVO retention = getDataRetentionSettings();
        items.add(summaryItem("data-retention", "数据保留", true, retention.getUpdateTime()));

        EcCompanyInfoVO company = getCompanyInfo();
        items.add(summaryItem("company-info", "公司信息",
                StringUtils.hasText(company.getCompanyName()), company.getUpdateTime()));

        return items;
    }

    @Override
    public EcInventorySettingsVO getInventorySettings() {
        return readConfig(KEY_INVENTORY, EcInventorySettingsVO.class, defaultInventorySettings());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcInventorySettingsVO saveInventorySettings(EcInventorySettingsSaveRequest request) {
        validateInventory(request);
        EcInventorySettingsVO vo = new EcInventorySettingsVO();
        vo.setDefaultAlertThreshold(request.getDefaultAlertThreshold());
        vo.setSlowMovingDays(request.getSlowMovingDays());
        vo.setSlowMovingFallbackDays(request.getSlowMovingFallbackDays());
        return writeConfig(KEY_INVENTORY, vo, EcInventorySettingsVO.class);
    }

    @Override
    public int resolveDefaultAlertThreshold() {
        Integer value = getInventorySettings().getDefaultAlertThreshold();
        return value != null && value >= 0 ? value : 10;
    }

    @Override
    public int resolveSlowMovingDays() {
        Integer value = getInventorySettings().getSlowMovingDays();
        return value != null && value > 0 ? value : 45;
    }

    @Override
    public int resolveSlowMovingFallbackDays() {
        Integer value = getInventorySettings().getSlowMovingFallbackDays();
        return value != null && value > 0 ? value : 90;
    }

    @Override
    public EcOrderImportSettingsVO getOrderImportSettings() {
        return readConfig(KEY_ORDER_IMPORT, EcOrderImportSettingsVO.class, defaultOrderImportSettings());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcOrderImportSettingsVO saveOrderImportSettings(EcOrderImportSettingsSaveRequest request) {
        if (request.getDataStartRow() <= request.getHeaderRow()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "数据起始行必须大于表头行");
        }
        EcOrderImportSettingsVO vo = new EcOrderImportSettingsVO();
        vo.setHeaderRow(request.getHeaderRow());
        vo.setDataStartRow(request.getDataStartRow());
        vo.setDateFormat(request.getDateFormat().trim());
        return writeConfig(KEY_ORDER_IMPORT, vo, EcOrderImportSettingsVO.class);
    }

    @Override
    public String resolveOrderImportDateFormat() {
        String format = getOrderImportSettings().getDateFormat();
        return StringUtils.hasText(format) ? format.trim() : "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    public EcOrderImportStatusSettingsVO getOrderImportStatusSettings() {
        EcOrderImportStatusSettingsVO config = readConfig(KEY_ORDER_IMPORT_STATUS,
                EcOrderImportStatusSettingsVO.class, defaultOrderImportStatusSettings());
        if (config.getStatusMapping() == null || config.getStatusMapping().isEmpty()) {
            config.setStatusMapping(new LinkedHashMap<>(EcSettingsBuiltinSupport.defaultOrderImportStatusMapping()));
        }
        if (!StringUtils.hasText(config.getDefaultLineStatus())) {
            config.setDefaultLineStatus("PAID");
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcOrderImportStatusSettingsVO saveOrderImportStatusSettings(EcOrderImportStatusSettingsSaveRequest request) {
        EcOrderImportStatusSettingsVO vo = new EcOrderImportStatusSettingsVO();
        vo.setDefaultLineStatus(request.getDefaultLineStatus().trim().toUpperCase());
        vo.setStatusMapping(sanitizeStatusMapping(request.getStatusMapping()));
        return writeConfig(KEY_ORDER_IMPORT_STATUS, vo, EcOrderImportStatusSettingsVO.class);
    }

    @Override
    public Map<String, String> resolveOrderImportStatusMapping() {
        return new LinkedHashMap<>(getOrderImportStatusSettings().getStatusMapping());
    }

    @Override
    public String resolveOrderImportDefaultLineStatus() {
        return getOrderImportStatusSettings().getDefaultLineStatus();
    }

    @Override
    public EcExpressSettingsVO getExpressSettings() {
        return readConfig(KEY_EXPRESS, EcExpressSettingsVO.class, defaultExpressSettings());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcExpressSettingsVO saveExpressSettings(EcExpressSettingsSaveRequest request) {
        if (request.getDataStartRow() <= request.getHeaderRow()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "数据起始行必须大于表头行");
        }
        EcExpressSettingsVO vo = new EcExpressSettingsVO();
        vo.setHeaderRow(request.getHeaderRow());
        vo.setDataStartRow(request.getDataStartRow());
        vo.setIncludeLabelPriceDefault(Boolean.TRUE.equals(request.getIncludeLabelPriceDefault()));
        return writeConfig(KEY_EXPRESS, vo, EcExpressSettingsVO.class);
    }

    @Override
    public boolean resolveIncludeLabelPriceDefault() {
        return Boolean.TRUE.equals(getExpressSettings().getIncludeLabelPriceDefault());
    }

    @Override
    public EcDeliveryNoteConfigVO getDeliveryNoteConfig() {
        EcDeliveryNoteConfigVO config = readConfig(KEY_DELIVERY_NOTE, EcDeliveryNoteConfigVO.class, defaultDeliveryNoteConfig());
        if (!StringUtils.hasText(config.getTitle())) {
            config.setTitle("唯十嘉送货单");
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcDeliveryNoteConfigVO saveDeliveryNoteConfig(EcDeliveryNoteConfigSaveRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "送货单标题不能为空");
        }
        EcDeliveryNoteConfigVO vo = new EcDeliveryNoteConfigVO();
        vo.setTitle(request.getTitle().trim());
        vo.setAddress(trimToNull(request.getAddress()));
        vo.setTel(trimToNull(request.getTel()));
        vo.setPreparedBy(trimToNull(request.getPreparedBy()));
        vo.setShipFromName(trimToNull(request.getShipFromName()));
        vo.setShipFromPhone(trimToNull(request.getShipFromPhone()));
        vo.setShipFromAddress(trimToNull(request.getShipFromAddress()));
        vo.setRequirementItems(normalizeList(request.getRequirementItems()));
        vo.setNoteItems(normalizeList(request.getNoteItems()));
        return writeConfig(KEY_DELIVERY_NOTE, vo, EcDeliveryNoteConfigVO.class);
    }

    @Override
    public EcOutboundOrderConfigVO getOutboundOrderConfig() {
        EcOutboundOrderConfigVO config = readConfig(KEY_OUTBOUND_ORDER, EcOutboundOrderConfigVO.class, defaultOutboundOrderConfig());
        if (!StringUtils.hasText(config.getTitle())) {
            config.setTitle("唯十嘉出库单");
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcOutboundOrderConfigVO saveOutboundOrderConfig(EcOutboundOrderConfigSaveRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "出库单标题不能为空");
        }
        EcOutboundOrderConfigVO vo = new EcOutboundOrderConfigVO();
        vo.setTitle(request.getTitle().trim());
        vo.setAddress(trimToNull(request.getAddress()));
        vo.setTel(trimToNull(request.getTel()));
        vo.setPreparedBy(trimToNull(request.getPreparedBy()));
        vo.setApprovedBy(trimToNull(request.getApprovedBy()));
        vo.setWarehouseKeeper(trimToNull(request.getWarehouseKeeper()));
        vo.setRequirementItems(normalizeList(request.getRequirementItems()));
        vo.setNoteItems(normalizeList(request.getNoteItems()));
        return writeConfig(KEY_OUTBOUND_ORDER, vo, EcOutboundOrderConfigVO.class);
    }

    @Override
    public EcSettlementSettingsVO getSettlementSettings() {
        return readConfig(KEY_SETTLEMENT, EcSettlementSettingsVO.class, defaultSettlementSettings());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSettlementSettingsVO saveSettlementSettings(EcSettlementSettingsSaveRequest request) {
        EcSettlementSettingsVO vo = new EcSettlementSettingsVO();
        vo.setProfitDisplayMode(request.getProfitDisplayMode().trim().toUpperCase());
        vo.setCostIncludesFreight(Boolean.TRUE.equals(request.getCostIncludesFreight()));
        return writeConfig(KEY_SETTLEMENT, vo, EcSettlementSettingsVO.class);
    }

    @Override
    public String resolveProfitDisplayMode() {
        String mode = getSettlementSettings().getProfitDisplayMode();
        return StringUtils.hasText(mode) ? mode.trim().toUpperCase() : "ACTUAL_PREFERRED";
    }

    @Override
    public EcRebateSettingsVO getRebateSettings() {
        return readConfig(KEY_REBATE, EcRebateSettingsVO.class, defaultRebateSettings());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcRebateSettingsVO saveRebateSettings(EcRebateSettingsSaveRequest request) {
        EcRebateSettingsVO vo = new EcRebateSettingsVO();
        vo.setDefaultRebatePct(request.getDefaultRebatePct());
        return writeConfig(KEY_REBATE, vo, EcRebateSettingsVO.class);
    }

    @Override
    public BigDecimal resolveDefaultRebatePct() {
        BigDecimal value = getRebateSettings().getDefaultRebatePct();
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public EcNotificationSettingsVO getNotificationSettings() {
        return readConfig(KEY_NOTIFICATION, EcNotificationSettingsVO.class, defaultNotificationSettings());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcNotificationSettingsVO saveNotificationSettings(EcNotificationSettingsSaveRequest request) {
        EcNotificationSettingsVO vo = new EcNotificationSettingsVO();
        vo.setInventoryAlertEnabled(Boolean.TRUE.equals(request.getInventoryAlertEnabled()));
        vo.setZeroStockAlertEnabled(Boolean.TRUE.equals(request.getZeroStockAlertEnabled()));
        vo.setSettlementRemindEnabled(Boolean.TRUE.equals(request.getSettlementRemindEnabled()));
        vo.setSettlementRemindDayOfMonth(request.getSettlementRemindDayOfMonth());
        return writeConfig(KEY_NOTIFICATION, vo, EcNotificationSettingsVO.class);
    }

    @Override
    public EcDataRetentionSettingsVO getDataRetentionSettings() {
        return readConfig(KEY_DATA_RETENTION, EcDataRetentionSettingsVO.class, defaultDataRetentionSettings());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcDataRetentionSettingsVO saveDataRetentionSettings(EcDataRetentionSettingsSaveRequest request) {
        EcDataRetentionSettingsVO vo = new EcDataRetentionSettingsVO();
        vo.setImportHistoryRetentionDays(request.getImportHistoryRetentionDays());
        vo.setInventoryLogRetentionDays(request.getInventoryLogRetentionDays());
        vo.setAutoCleanupEnabled(Boolean.TRUE.equals(request.getAutoCleanupEnabled()));
        return writeConfig(KEY_DATA_RETENTION, vo, EcDataRetentionSettingsVO.class);
    }

    @Override
    public EcCompanyInfoVO getCompanyInfo() {
        return readConfig(KEY_COMPANY, EcCompanyInfoVO.class, defaultCompanyInfo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcCompanyInfoVO saveCompanyInfo(EcCompanyInfoSaveRequest request) {
        EcCompanyInfoVO vo = new EcCompanyInfoVO();
        vo.setCompanyName(trimToNull(request.getCompanyName()));
        vo.setAddress(trimToNull(request.getAddress()));
        vo.setTel(trimToNull(request.getTel()));
        vo.setContactName(trimToNull(request.getContactName()));
        vo.setContactPhone(trimToNull(request.getContactPhone()));
        return writeConfig(KEY_COMPANY, vo, EcCompanyInfoVO.class);
    }

    private EcSettingsSummaryItemVO summaryItem(String key, String label, boolean configured,
                                                java.time.LocalDateTime updateTime) {
        EcSettingsSummaryItemVO item = new EcSettingsSummaryItemVO();
        item.setKey(key);
        item.setLabel(label);
        item.setConfigured(configured);
        item.setUpdateTime(updateTime);
        return item;
    }

    private void validateInventory(EcInventorySettingsSaveRequest request) {
        if (request.getDefaultAlertThreshold() == null || request.getDefaultAlertThreshold() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "默认预警阈值无效");
        }
        if (request.getSlowMovingDays() == null || request.getSlowMovingDays() < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "滞销判定天数无效");
        }
        if (request.getSlowMovingFallbackDays() == null || request.getSlowMovingFallbackDays() < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "滞销兜底天数无效");
        }
    }

    private <T> T readConfig(String key, Class<T> type, T fallback) {
        EcSystemConfig row = getById(key);
        if (row == null || !StringUtils.hasText(row.getConfigJson())) {
            T value = copyFallback(fallback, type);
            if (row != null) {
                setUpdateTime(value, row.getUpdateTime());
            }
            return value;
        }
        try {
            T value = objectMapper.readValue(row.getConfigJson(), type);
            setUpdateTime(value, row.getUpdateTime());
            return value;
        } catch (JsonProcessingException ex) {
            T value = copyFallback(fallback, type);
            setUpdateTime(value, row.getUpdateTime());
            return value;
        }
    }

    private <T> T writeConfig(String key, T payload, Class<T> type) {
        EcSystemConfig row = getOrCreate(key, payload, type);
        try {
            row.setConfigJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "配置序列化失败");
        }
        saveOrUpdate(row);
        EcSystemConfig saved = getById(key);
        T value = readConfig(key, type, payload);
        setUpdateTime(value, saved != null ? saved.getUpdateTime() : null);
        return value;
    }

    private EcSystemConfig getOrCreate(String key, Object fallback, Class<?> type) {
        EcSystemConfig row = getById(key);
        if (row != null) {
            return row;
        }
        row = new EcSystemConfig();
        row.setConfigKey(key);
        try {
            row.setConfigJson(objectMapper.writeValueAsString(fallback));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "默认配置序列化失败");
        }
        save(row);
        return row;
    }

    private <T> T copyFallback(T fallback, Class<T> type) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(fallback), type);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "配置复制失败");
        }
    }

    private void setUpdateTime(Object value, java.time.LocalDateTime updateTime) {
        if (value instanceof EcInventorySettingsVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcOrderImportSettingsVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcExpressSettingsVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcDeliveryNoteConfigVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcCompanyInfoVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcOrderImportStatusSettingsVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcOutboundOrderConfigVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcSettlementSettingsVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcRebateSettingsVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcNotificationSettingsVO vo) {
            vo.setUpdateTime(updateTime);
        } else if (value instanceof EcDataRetentionSettingsVO vo) {
            vo.setUpdateTime(updateTime);
        }
    }

    private EcInventorySettingsVO defaultInventorySettings() {
        EcInventorySettingsVO vo = new EcInventorySettingsVO();
        vo.setDefaultAlertThreshold(10);
        vo.setSlowMovingDays(45);
        vo.setSlowMovingFallbackDays(90);
        return vo;
    }

    private EcOrderImportSettingsVO defaultOrderImportSettings() {
        EcOrderImportSettingsVO vo = new EcOrderImportSettingsVO();
        vo.setHeaderRow(1);
        vo.setDataStartRow(2);
        vo.setDateFormat("yyyy-MM-dd HH:mm:ss");
        return vo;
    }

    private EcOrderImportStatusSettingsVO defaultOrderImportStatusSettings() {
        EcOrderImportStatusSettingsVO vo = new EcOrderImportStatusSettingsVO();
        vo.setDefaultLineStatus("PAID");
        vo.setStatusMapping(new LinkedHashMap<>(EcSettingsBuiltinSupport.defaultOrderImportStatusMapping()));
        return vo;
    }

    private EcExpressSettingsVO defaultExpressSettings() {
        EcExpressSettingsVO vo = new EcExpressSettingsVO();
        vo.setHeaderRow(1);
        vo.setDataStartRow(2);
        vo.setIncludeLabelPriceDefault(false);
        return vo;
    }

    private EcDeliveryNoteConfigVO defaultDeliveryNoteConfig() {
        EcDeliveryNoteConfigVO vo = new EcDeliveryNoteConfigVO();
        vo.setTitle("唯十嘉送货单");
        vo.setRequirementItems(new ArrayList<>());
        vo.setNoteItems(new ArrayList<>());
        return vo;
    }

    private EcOutboundOrderConfigVO defaultOutboundOrderConfig() {
        EcOutboundOrderConfigVO vo = new EcOutboundOrderConfigVO();
        vo.setTitle("唯十嘉出库单");
        vo.setRequirementItems(new ArrayList<>());
        vo.setNoteItems(new ArrayList<>());
        return vo;
    }

    private EcSettlementSettingsVO defaultSettlementSettings() {
        EcSettlementSettingsVO vo = new EcSettlementSettingsVO();
        vo.setProfitDisplayMode("ACTUAL_PREFERRED");
        vo.setCostIncludesFreight(true);
        return vo;
    }

    private EcRebateSettingsVO defaultRebateSettings() {
        EcRebateSettingsVO vo = new EcRebateSettingsVO();
        vo.setDefaultRebatePct(BigDecimal.ZERO);
        return vo;
    }

    private EcNotificationSettingsVO defaultNotificationSettings() {
        EcNotificationSettingsVO vo = new EcNotificationSettingsVO();
        vo.setInventoryAlertEnabled(true);
        vo.setZeroStockAlertEnabled(true);
        vo.setSettlementRemindEnabled(true);
        vo.setSettlementRemindDayOfMonth(25);
        return vo;
    }

    private EcDataRetentionSettingsVO defaultDataRetentionSettings() {
        EcDataRetentionSettingsVO vo = new EcDataRetentionSettingsVO();
        vo.setImportHistoryRetentionDays(365);
        vo.setInventoryLogRetentionDays(180);
        vo.setAutoCleanupEnabled(false);
        return vo;
    }

    private Map<String, String> sanitizeStatusMapping(Map<String, String> mapping) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        if (mapping == null) {
            return sanitized;
        }
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (!StringUtils.hasText(entry.getKey()) || !StringUtils.hasText(entry.getValue())) {
                continue;
            }
            sanitized.put(entry.getKey().trim(), entry.getValue().trim().toUpperCase());
        }
        return sanitized;
    }

    private EcCompanyInfoVO defaultCompanyInfo() {
        return new EcCompanyInfoVO();
    }

    private List<String> normalizeList(List<String> items) {
        List<String> normalized = new ArrayList<>();
        if (items == null) {
            return normalized;
        }
        for (String item : items) {
            if (StringUtils.hasText(item)) {
                normalized.add(item.trim());
            }
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
