package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.EcSettlementBuyerExcludeSaveRequest;
import com.ai.manager.system.domain.dto.EcSettlementExpressBillManualSaveRequest;
import com.ai.manager.system.domain.dto.EcSettlementOrderDecisionBatchRequest;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO;
import com.ai.manager.system.domain.vo.EcSettlementBuyerExcludeVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillImportVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillLineVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillPreviewVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillRecordVO;
import com.ai.manager.system.service.EcMonthlySettlementService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecommerce/monthly-settlement")
@RequiredArgsConstructor
public class EcMonthlySettlementController {

    private final EcMonthlySettlementService monthlySettlementService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ApiResult<EcMonthlySettlementVO> calculate(@RequestParam String month,
                                                      @RequestParam(required = false) Long shopId) {
        return ApiResult.ok(monthlySettlementService.calculate(month, shopId));
    }

    @GetMapping("/snapshot")
    public ApiResult<EcMonthlySettlementVO> loadSnapshot(@RequestParam String month) {
        return ApiResult.ok(monthlySettlementService.loadSnapshot(month));
    }

    @PostMapping("/calculate")
    public ApiResult<EcMonthlySettlementVO> calculateAndSave(@RequestParam String month) {
        return ApiResult.ok(monthlySettlementService.calculateAndSave(month));
    }

    @GetMapping("/buyer-excludes")
    public ApiResult<List<EcSettlementBuyerExcludeVO>> listBuyerExcludes(
            @RequestParam(required = false) Long shopId) {
        return ApiResult.ok(monthlySettlementService.listBuyerExcludes(shopId));
    }

    @PostMapping("/buyer-excludes")
    public ApiResult<EcSettlementBuyerExcludeVO> saveBuyerExclude(
            @RequestBody EcSettlementBuyerExcludeSaveRequest request) {
        return ApiResult.ok(monthlySettlementService.saveBuyerExclude(request));
    }

    @DeleteMapping("/buyer-excludes/{id}")
    public ApiResult<Void> deleteBuyerExclude(@PathVariable Long id) {
        monthlySettlementService.deleteBuyerExclude(id);
        return ApiResult.ok();
    }

    @PostMapping("/order-decisions")
    public ApiResult<EcMonthlySettlementVO> saveOrderDecisions(
            @RequestBody EcSettlementOrderDecisionBatchRequest request) {
        return ApiResult.ok(monthlySettlementService.saveOrderDecisions(request));
    }

    @PostMapping("/express-bill/import")
    public ApiResult<EcSettlementExpressBillImportVO> importExpressBill(
            @RequestParam String month,
            @RequestParam Long expressStationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String columnMapping,
            @RequestParam(required = false) Integer headerRow,
            @RequestParam(required = false) Integer dataStartRow,
            @RequestParam(required = false, defaultValue = "false") Boolean includeLabelPrice) throws Exception {
        Map<String, String> mapping = parseColumnMapping(columnMapping);
        return ApiResult.ok(monthlySettlementService.importExpressBill(
                month, expressStationId, file, mapping, headerRow, dataStartRow, includeLabelPrice));
    }

    @PostMapping("/express-bill/manual/prepare")
    public ApiResult<EcSettlementExpressBillImportVO> prepareManualExpressBill(
            @RequestParam String month,
            @RequestParam Long expressStationId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeLabelPrice) {
        return ApiResult.ok(monthlySettlementService.prepareManualExpressBill(month, expressStationId, includeLabelPrice));
    }

    @PostMapping("/express-bill/manual/lines")
    public ApiResult<EcSettlementExpressBillImportVO> saveManualExpressBillLines(
            @RequestBody EcSettlementExpressBillManualSaveRequest request) {
        return ApiResult.ok(monthlySettlementService.saveManualExpressBillLines(request));
    }

    @GetMapping("/express-bill/manual/lines")
    public ApiResult<List<EcSettlementExpressBillLineVO>> listManualPendingLines(@RequestParam Long billId) {
        return ApiResult.ok(monthlySettlementService.listManualPendingLines(billId));
    }

    @GetMapping("/express-bill/unmatched-lines")
    public ApiResult<List<EcSettlementExpressBillLineVO>> listUnmatchedExpressBillLines(@RequestParam Long billId) {
        return ApiResult.ok(monthlySettlementService.listUnmatchedExpressBillLines(billId));
    }

    @GetMapping("/express-bill/records")
    public ApiResult<List<EcSettlementExpressBillRecordVO>> listExpressBillRecords(@RequestParam String month) {
        return ApiResult.ok(monthlySettlementService.listExpressBillRecords(month));
    }

    @PostMapping("/express-bill/preview-columns")
    public ApiResult<EcSettlementExpressBillPreviewVO> previewExpressBillColumns(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer headerRow) {
        return ApiResult.ok(monthlySettlementService.previewExpressBillColumns(file, headerRow));
    }

    @GetMapping("/express-bill/imported")
    public ApiResult<Boolean> expressBillImported(@RequestParam String month) {
        return ApiResult.ok(monthlySettlementService.isExpressBillImported(month));
    }

    private Map<String, String> parseColumnMapping(String columnMapping) throws Exception {
        if (!StringUtils.hasText(columnMapping)) {
            return null;
        }
        return objectMapper.readValue(columnMapping, new TypeReference<Map<String, String>>() {
        });
    }
}
