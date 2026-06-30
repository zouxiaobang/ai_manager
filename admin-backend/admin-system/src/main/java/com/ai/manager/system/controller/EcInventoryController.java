package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.system.domain.dto.EcInventoryAdjustRequest;
import com.ai.manager.system.domain.dto.EcInventoryInboundRequest;
import com.ai.manager.system.domain.dto.EcInventorySaveRequest;
import com.ai.manager.system.domain.vo.EcInventoryDetailVO;
import com.ai.manager.system.domain.vo.EcInventoryFactorySummaryVO;
import com.ai.manager.system.domain.vo.EcInventoryGlobalLogVO;
import com.ai.manager.system.domain.vo.EcInventoryInboundValueSummaryVO;
import com.ai.manager.system.domain.vo.EcInventoryListItemVO;
import com.ai.manager.system.domain.vo.EcInventoryLogVO;
import com.ai.manager.system.domain.vo.EcInventoryPackingEstimateVO;
import com.ai.manager.system.domain.vo.EcInventorySkuOptionVO;
import com.ai.manager.system.service.EcInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/inventories")
@RequiredArgsConstructor
public class EcInventoryController {

    private final EcInventoryService ecInventoryService;

    @GetMapping
    public ApiResult<PageResult<EcInventoryListItemVO>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Boolean alertOnly,
                                                               @RequestParam(required = false) Boolean inStockOnly,
                                                               @RequestParam(required = false) Long factoryId,
                                                               @RequestParam(required = false) Long page,
                                                               @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecInventoryService.pageInventories(keyword, alertOnly, inStockOnly, factoryId, page, pageSize));
    }

    @GetMapping("/logs")
    public ApiResult<PageResult<EcInventoryGlobalLogVO>> globalLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) Long factoryId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String refType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecInventoryService.pageGlobalLogs(
                keyword, skuCode, factoryId, changeType, refType, startTime, endTime, page, pageSize));
    }

    @GetMapping("/factory-summary")
    public ApiResult<List<EcInventoryFactorySummaryVO>> factorySummary(
            @RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecInventoryService.listFactorySummary(factoryId));
    }

    @GetMapping("/inbound-value-summary")
    public ApiResult<EcInventoryInboundValueSummaryVO> inboundValueSummary(
            @RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecInventoryService.summarizeHistoricalInboundValue(factoryId));
    }

    @GetMapping("/packing-estimate")
    public ApiResult<EcInventoryPackingEstimateVO> packingEstimate(
            @RequestParam String skuCode,
            @RequestParam(required = false) Integer outboundQty) {
        return ApiResult.ok(ecInventoryService.estimatePacking(skuCode, outboundQty));
    }

    @GetMapping("/sku-options")
    public ApiResult<List<EcInventorySkuOptionVO>> skuOptions(@RequestParam(required = false) Long factoryId,
                                                              @RequestParam(required = false) Long productId,
                                                              @RequestParam(required = false) String productIds,
                                                              @RequestParam(required = false) String keyword) {
        List<Long> parsedProductIds = parseProductIds(productIds);
        return ApiResult.ok(ecInventoryService.listSkuOptions(factoryId, productId, parsedProductIds, keyword));
    }

    private static List<Long> parseProductIds(String productIds) {
        if (productIds == null || productIds.isBlank()) {
            return List.of();
        }
        List<Long> result = new java.util.ArrayList<>();
        for (String part : productIds.split(",")) {
            if (part == null || part.isBlank()) {
                continue;
            }
            try {
                result.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                /* skip invalid */
            }
        }
        return result;
    }

    @PostMapping("/inbound")
    public ApiResult<EcInventoryDetailVO> quickInbound(@RequestBody EcInventoryInboundRequest request) {
        return ApiResult.ok(ecInventoryService.quickInbound(request));
    }

    @GetMapping("/available-sku-codes")
    public ApiResult<List<String>> availableSkuCodes() {
        return ApiResult.ok(ecInventoryService.listAvailableSkuCodes());
    }

    @GetMapping("/{id}")
    public ApiResult<EcInventoryDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecInventoryService.getInventoryDetail(id));
    }

    @GetMapping("/{id}/logs")
    public ApiResult<PageResult<EcInventoryLogVO>> logs(@PathVariable Long id,
                                                          @RequestParam(required = false) Long page,
                                                          @RequestParam(required = false) Long pageSize) {
        if (page != null || pageSize != null) {
            return ApiResult.ok(ecInventoryService.pageLogs(id, page, pageSize));
        }
        List<EcInventoryLogVO> all = ecInventoryService.listLogs(id);
        return ApiResult.ok(PageUtils.of(all, all.size(), 1L, all.size()));
    }

    @PostMapping
    public ApiResult<EcInventoryListItemVO> create(@RequestBody EcInventorySaveRequest request) {
        return ApiResult.ok(ecInventoryService.createInventory(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcInventoryListItemVO> update(@PathVariable Long id,
                                                   @RequestBody EcInventorySaveRequest request) {
        return ApiResult.ok(ecInventoryService.updateInventory(id, request));
    }

    @PostMapping("/{id}/adjust")
    public ApiResult<EcInventoryListItemVO> adjust(@PathVariable Long id,
                                                   @RequestBody EcInventoryAdjustRequest request) {
        return ApiResult.ok(ecInventoryService.adjustInventory(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecInventoryService.deleteInventory(id);
        return ApiResult.ok();
    }
}
