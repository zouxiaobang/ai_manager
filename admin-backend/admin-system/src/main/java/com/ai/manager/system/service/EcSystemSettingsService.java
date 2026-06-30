package com.ai.manager.system.service;

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
import com.ai.manager.system.domain.vo.EcCompanyInfoVO;
import com.ai.manager.system.domain.vo.EcDataRetentionSettingsVO;
import com.ai.manager.system.domain.vo.EcDeliveryNoteConfigVO;
import com.ai.manager.system.domain.vo.EcExpressSettingsVO;
import com.ai.manager.system.domain.vo.EcInventorySettingsVO;
import com.ai.manager.system.domain.vo.EcNotificationSettingsVO;
import com.ai.manager.system.domain.vo.EcOrderImportSettingsVO;
import com.ai.manager.system.domain.vo.EcOrderImportStatusSettingsVO;
import com.ai.manager.system.domain.vo.EcOutboundOrderConfigVO;
import com.ai.manager.system.domain.vo.EcRebateSettingsVO;
import com.ai.manager.system.domain.vo.EcSettlementSettingsVO;
import com.ai.manager.system.domain.vo.EcSettingsSummaryItemVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface EcSystemSettingsService {

    List<EcSettingsSummaryItemVO> listSummary();

    EcInventorySettingsVO getInventorySettings();

    EcInventorySettingsVO saveInventorySettings(EcInventorySettingsSaveRequest request);

    int resolveDefaultAlertThreshold();

    int resolveSlowMovingDays();

    int resolveSlowMovingFallbackDays();

    EcOrderImportSettingsVO getOrderImportSettings();

    EcOrderImportSettingsVO saveOrderImportSettings(EcOrderImportSettingsSaveRequest request);

    String resolveOrderImportDateFormat();

    EcOrderImportStatusSettingsVO getOrderImportStatusSettings();

    EcOrderImportStatusSettingsVO saveOrderImportStatusSettings(EcOrderImportStatusSettingsSaveRequest request);

    Map<String, String> resolveOrderImportStatusMapping();

    String resolveOrderImportDefaultLineStatus();

    EcExpressSettingsVO getExpressSettings();

    EcExpressSettingsVO saveExpressSettings(EcExpressSettingsSaveRequest request);

    boolean resolveIncludeLabelPriceDefault();

    EcDeliveryNoteConfigVO getDeliveryNoteConfig();

    EcDeliveryNoteConfigVO saveDeliveryNoteConfig(EcDeliveryNoteConfigSaveRequest request);

    EcOutboundOrderConfigVO getOutboundOrderConfig();

    EcOutboundOrderConfigVO saveOutboundOrderConfig(EcOutboundOrderConfigSaveRequest request);

    EcSettlementSettingsVO getSettlementSettings();

    EcSettlementSettingsVO saveSettlementSettings(EcSettlementSettingsSaveRequest request);

    String resolveProfitDisplayMode();

    EcRebateSettingsVO getRebateSettings();

    EcRebateSettingsVO saveRebateSettings(EcRebateSettingsSaveRequest request);

    BigDecimal resolveDefaultRebatePct();

    EcNotificationSettingsVO getNotificationSettings();

    EcNotificationSettingsVO saveNotificationSettings(EcNotificationSettingsSaveRequest request);

    EcDataRetentionSettingsVO getDataRetentionSettings();

    EcDataRetentionSettingsVO saveDataRetentionSettings(EcDataRetentionSettingsSaveRequest request);

    EcCompanyInfoVO getCompanyInfo();

    EcCompanyInfoVO saveCompanyInfo(EcCompanyInfoSaveRequest request);
}
