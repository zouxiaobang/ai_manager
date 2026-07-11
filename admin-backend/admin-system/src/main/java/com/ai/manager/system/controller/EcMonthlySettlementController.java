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

/**
 * 电商月度结算控制器
 *
 * <p>所属模块：电商模块-月度结算</p>
 * <p>API路径前缀：/api/ecommerce/monthly-settlement</p>
 * <p>功能描述：提供月度结算计算、买家排除管理、快递对账单导入、订单决策等结算管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/monthly-settlement")
@RequiredArgsConstructor
public class EcMonthlySettlementController {

    private final EcMonthlySettlementService monthlySettlementService;
    private final ObjectMapper objectMapper;

    /**
     * 计算月度结算
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/monthly-settlement</p>
     *
     * @param month 月份
     * @param shopId 店铺ID
     * @return 月度结算结果
     */
    @GetMapping
    public ApiResult<EcMonthlySettlementVO> calculate(@RequestParam String month,
                                                      @RequestParam(required = false) Long shopId) {
        return ApiResult.ok(monthlySettlementService.calculate(month, shopId));
    }

    /**
     * 加载月度结算快照
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/monthly-settlement/snapshot</p>
     *
     * @param month 月份
     * @return 月度结算快照
     */
    @GetMapping("/snapshot")
    public ApiResult<EcMonthlySettlementVO> loadSnapshot(@RequestParam String month) {
        return ApiResult.ok(monthlySettlementService.loadSnapshot(month));
    }

    /**
     * 计算并保存月度结算
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/monthly-settlement/calculate</p>
     *
     * @param month 月份
     * @return 月度结算结果
     */
    @PostMapping("/calculate")
    public ApiResult<EcMonthlySettlementVO> calculateAndSave(@RequestParam String month) {
        return ApiResult.ok(monthlySettlementService.calculateAndSave(month));
    }

    /**
     * 获取买家排除列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/monthly-settlement/buyer-excludes</p>
     *
     * @param shopId 店铺ID
     * @return 买家排除列表
     */
    @GetMapping("/buyer-excludes")
    public ApiResult<List<EcSettlementBuyerExcludeVO>> listBuyerExcludes(
            @RequestParam(required = false) Long shopId) {
        return ApiResult.ok(monthlySettlementService.listBuyerExcludes(shopId));
    }

    /**
     * 保存买家排除
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/monthly-settlement/buyer-excludes</p>
     *
     * @param request 买家排除保存请求参数
     * @return 买家排除信息
     */
    @PostMapping("/buyer-excludes")
    public ApiResult<EcSettlementBuyerExcludeVO> saveBuyerExclude(
            @RequestBody EcSettlementBuyerExcludeSaveRequest request) {
        return ApiResult.ok(monthlySettlementService.saveBuyerExclude(request));
    }

    /**
     * 删除买家排除
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/monthly-settlement/buyer-excludes/{id}</p>
     *
     * @param id 买家排除ID
     * @return 操作结果
     */
    @DeleteMapping("/buyer-excludes/{id}")
    public ApiResult<Void> deleteBuyerExclude(@PathVariable Long id) {
        monthlySettlementService.deleteBuyerExclude(id);
        return ApiResult.ok();
    }

    /**
     * 保存订单决策
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/monthly-settlement/order-decisions</p>
     *
     * @param request 订单决策批量请求参数
     * @return 月度结算结果
     */
    @PostMapping("/order-decisions")
    public ApiResult<EcMonthlySettlementVO> saveOrderDecisions(
            @RequestBody EcSettlementOrderDecisionBatchRequest request) {
        return ApiResult.ok(monthlySettlementService.saveOrderDecisions(request));
    }

    /**
     * 导入快递对账单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/monthly-settlement/express-bill/import</p>
     *
     * @param month 月份
     * @param expressStationId 快递站点ID
     * @param file 对账单文件
     * @param columnMapping 列映射
     * @param headerRow 表头行号
     * @param dataStartRow 数据起始行号
     * @param includeLabelPrice 是否包含面单价
     * @return 导入结果
     * @throws Exception 导入异常
     */
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

    /**
     * 准备手工快递对账单
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/monthly-settlement/express-bill/manual/prepare</p>
     *
     * @param month 月份
     * @param expressStationId 快递站点ID
     * @param includeLabelPrice 是否包含面单价
     * @return 导入结果
     */
    @PostMapping("/express-bill/manual/prepare")
    public ApiResult<EcSettlementExpressBillImportVO> prepareManualExpressBill(
            @RequestParam String month,
            @RequestParam Long expressStationId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeLabelPrice) {
        return ApiResult.ok(monthlySettlementService.prepareManualExpressBill(month, expressStationId, includeLabelPrice));
    }

    /**
     * 保存手工快递对账单明细
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/monthly-settlement/express-bill/manual/lines</p>
     *
     * @param request 手工快递对账单保存请求参数
     * @return 导入结果
     */
    @PostMapping("/express-bill/manual/lines")
    public ApiResult<EcSettlementExpressBillImportVO> saveManualExpressBillLines(
            @RequestBody EcSettlementExpressBillManualSaveRequest request) {
        return ApiResult.ok(monthlySettlementService.saveManualExpressBillLines(request));
    }

    /**
     * 获取手工待处理明细列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/monthly-settlement/express-bill/manual/lines</p>
     *
     * @param billId 对账单ID
     * @return 手工待处理明细列表
     */
    @GetMapping("/express-bill/manual/lines")
    public ApiResult<List<EcSettlementExpressBillLineVO>> listManualPendingLines(@RequestParam Long billId) {
        return ApiResult.ok(monthlySettlementService.listManualPendingLines(billId));
    }

    /**
     * 获取未匹配的快递对账单明细列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/monthly-settlement/express-bill/unmatched-lines</p>
     *
     * @param billId 对账单ID
     * @return 未匹配明细列表
     */
    @GetMapping("/express-bill/unmatched-lines")
    public ApiResult<List<EcSettlementExpressBillLineVO>> listUnmatchedExpressBillLines(@RequestParam Long billId) {
        return ApiResult.ok(monthlySettlementService.listUnmatchedExpressBillLines(billId));
    }

    /**
     * 获取快递对账单记录列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/monthly-settlement/express-bill/records</p>
     *
     * @param month 月份
     * @return 对账单记录列表
     */
    @GetMapping("/express-bill/records")
    public ApiResult<List<EcSettlementExpressBillRecordVO>> listExpressBillRecords(@RequestParam String month) {
        return ApiResult.ok(monthlySettlementService.listExpressBillRecords(month));
    }

    /**
     * 预览快递对账单列
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/monthly-settlement/express-bill/preview-columns</p>
     *
     * @param file 对账单文件
     * @param headerRow 表头行号
     * @return 列预览结果
     */
    @PostMapping("/express-bill/preview-columns")
    public ApiResult<EcSettlementExpressBillPreviewVO> previewExpressBillColumns(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer headerRow) {
        return ApiResult.ok(monthlySettlementService.previewExpressBillColumns(file, headerRow));
    }

    /**
     * 检查快递对账单是否已导入
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/monthly-settlement/express-bill/imported</p>
     *
     * @param month 月份
     * @return 是否已导入
     */
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
