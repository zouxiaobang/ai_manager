package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
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
import com.ai.manager.system.service.EcSystemSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/settings")
@RequiredArgsConstructor
public class EcSystemSettingsController {

    private final EcSystemSettingsService ecSystemSettingsService;

    @GetMapping("/summary")
    public ApiResult<List<EcSettingsSummaryItemVO>> summary() {
        return ApiResult.ok(ecSystemSettingsService.listSummary());
    }

    @GetMapping("/inventory")
    public ApiResult<EcInventorySettingsVO> getInventory() {
        return ApiResult.ok(ecSystemSettingsService.getInventorySettings());
    }

    @PutMapping("/inventory")
    public ApiResult<EcInventorySettingsVO> saveInventory(@Valid @RequestBody EcInventorySettingsSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveInventorySettings(request));
    }

    @GetMapping("/order-import")
    public ApiResult<EcOrderImportSettingsVO> getOrderImport() {
        return ApiResult.ok(ecSystemSettingsService.getOrderImportSettings());
    }

    @PutMapping("/order-import")
    public ApiResult<EcOrderImportSettingsVO> saveOrderImport(@Valid @RequestBody EcOrderImportSettingsSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveOrderImportSettings(request));
    }

    @GetMapping("/order-import-status")
    public ApiResult<EcOrderImportStatusSettingsVO> getOrderImportStatus() {
        return ApiResult.ok(ecSystemSettingsService.getOrderImportStatusSettings());
    }

    @PutMapping("/order-import-status")
    public ApiResult<EcOrderImportStatusSettingsVO> saveOrderImportStatus(
            @Valid @RequestBody EcOrderImportStatusSettingsSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveOrderImportStatusSettings(request));
    }

    @GetMapping("/express")
    public ApiResult<EcExpressSettingsVO> getExpress() {
        return ApiResult.ok(ecSystemSettingsService.getExpressSettings());
    }

    @PutMapping("/express")
    public ApiResult<EcExpressSettingsVO> saveExpress(@Valid @RequestBody EcExpressSettingsSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveExpressSettings(request));
    }

    @GetMapping("/delivery-note")
    public ApiResult<EcDeliveryNoteConfigVO> getDeliveryNote() {
        return ApiResult.ok(ecSystemSettingsService.getDeliveryNoteConfig());
    }

    @PutMapping("/delivery-note")
    public ApiResult<EcDeliveryNoteConfigVO> saveDeliveryNote(@Valid @RequestBody EcDeliveryNoteConfigSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveDeliveryNoteConfig(request));
    }

    @GetMapping("/outbound-order")
    public ApiResult<EcOutboundOrderConfigVO> getOutboundOrder() {
        return ApiResult.ok(ecSystemSettingsService.getOutboundOrderConfig());
    }

    @PutMapping("/outbound-order")
    public ApiResult<EcOutboundOrderConfigVO> saveOutboundOrder(@Valid @RequestBody EcOutboundOrderConfigSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveOutboundOrderConfig(request));
    }

    @GetMapping("/settlement")
    public ApiResult<EcSettlementSettingsVO> getSettlement() {
        return ApiResult.ok(ecSystemSettingsService.getSettlementSettings());
    }

    @PutMapping("/settlement")
    public ApiResult<EcSettlementSettingsVO> saveSettlement(@Valid @RequestBody EcSettlementSettingsSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveSettlementSettings(request));
    }

    @GetMapping("/rebate")
    public ApiResult<EcRebateSettingsVO> getRebate() {
        return ApiResult.ok(ecSystemSettingsService.getRebateSettings());
    }

    @PutMapping("/rebate")
    public ApiResult<EcRebateSettingsVO> saveRebate(@Valid @RequestBody EcRebateSettingsSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveRebateSettings(request));
    }

    @GetMapping("/notification")
    public ApiResult<EcNotificationSettingsVO> getNotification() {
        return ApiResult.ok(ecSystemSettingsService.getNotificationSettings());
    }

    @PutMapping("/notification")
    public ApiResult<EcNotificationSettingsVO> saveNotification(@Valid @RequestBody EcNotificationSettingsSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveNotificationSettings(request));
    }

    @GetMapping("/data-retention")
    public ApiResult<EcDataRetentionSettingsVO> getDataRetention() {
        return ApiResult.ok(ecSystemSettingsService.getDataRetentionSettings());
    }

    @PutMapping("/data-retention")
    public ApiResult<EcDataRetentionSettingsVO> saveDataRetention(@Valid @RequestBody EcDataRetentionSettingsSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveDataRetentionSettings(request));
    }

    @GetMapping("/company")
    public ApiResult<EcCompanyInfoVO> getCompany() {
        return ApiResult.ok(ecSystemSettingsService.getCompanyInfo());
    }

    @PutMapping("/company")
    public ApiResult<EcCompanyInfoVO> saveCompany(@Valid @RequestBody EcCompanyInfoSaveRequest request) {
        return ApiResult.ok(ecSystemSettingsService.saveCompanyInfo(request));
    }
}
