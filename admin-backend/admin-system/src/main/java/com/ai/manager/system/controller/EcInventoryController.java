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
import com.ai.manager.system.domain.vo.EcInventoryOverviewVO;
import com.ai.manager.system.domain.vo.EcInventoryPackingEstimateVO;
import com.ai.manager.system.domain.vo.EcInventorySummaryVO;
import com.ai.manager.system.domain.vo.EcInventorySkuOptionVO;
import com.ai.manager.system.domain.vo.EcInventorySpuStatusVO;
import com.ai.manager.system.service.EcInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 电商库存控制器
 *
 * <p>所属模块：电商模块-库存管理</p>
 * <p>API路径前缀：/api/ecommerce/inventories</p>
 * <p>功能描述：提供库存查询、库存调整、快速入库、库存日志、装箱估算等库存管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/inventories")
@RequiredArgsConstructor
public class EcInventoryController {

    private final EcInventoryService ecInventoryService;

    /**
     * 分页查询库存列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories</p>
     *
     * @param keyword 关键词，用于搜索SKU、商品名称等
     * @param alertOnly 是否仅显示预警库存
     * @param inStockOnly 是否仅显示有库存
     * @param factoryId 工厂ID
     * @param page 页码
     * @param pageSize 每页条数
     * @param groupBySpu 是否按SPU分组
     * @return 库存分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcInventoryListItemVO>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Boolean alertOnly,
                                                               @RequestParam(required = false) Boolean inStockOnly,
                                                               @RequestParam(required = false) Long factoryId,
                                                               @RequestParam(required = false) Long page,
                                                               @RequestParam(required = false) Long pageSize,
                                                               @RequestParam(required = false) Boolean groupBySpu) {
        return ApiResult.ok(ecInventoryService.pageInventories(keyword, alertOnly, inStockOnly, factoryId, page, pageSize, groupBySpu));
    }

    /**
     * 获取库存概览
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/overview</p>
     *
     * @return 库存概览信息
     */
    @GetMapping("/overview")
    public ApiResult<EcInventoryOverviewVO> overview() {
        return ApiResult.ok(ecInventoryService.getInventoryOverview());
    }

    /**
     * 获取库存汇总
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/summary</p>
     *
     * @param factoryId 工厂ID
     * @return 库存汇总信息
     */
    @GetMapping("/summary")
    public ApiResult<EcInventorySummaryVO> summary(@RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecInventoryService.getInventorySummary(factoryId));
    }

    /**
     * 获取SPU状态数量统计
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/spu-status-counts</p>
     *
     * @return SPU状态数量统计
     */
    @GetMapping("/spu-status-counts")
    public ApiResult<EcInventorySpuStatusVO> spuStatusCounts() {
        return ApiResult.ok(ecInventoryService.getSpuStatusCounts());
    }

    /**
     * 根据商品查询库存
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/by-product</p>
     *
     * @param productId 商品ID
     * @return 库存列表
     */
    @GetMapping("/by-product")
    public ApiResult<List<EcInventoryListItemVO>> byProduct(@RequestParam Long productId) {
        return ApiResult.ok(ecInventoryService.getInventoryByProduct(productId));
    }

    /**
     * 分页查询全局库存日志
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/logs</p>
     *
     * @param keyword 关键词
     * @param skuCode SKU编码
     * @param factoryId 工厂ID
     * @param changeType 变动类型
     * @param refType 关联类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param page 页码
     * @param pageSize 每页条数
     * @return 库存日志分页结果
     */
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

    /**
     * 获取工厂库存汇总列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/factory-summary</p>
     *
     * @param factoryId 工厂ID
     * @return 工厂库存汇总列表
     */
    @GetMapping("/factory-summary")
    public ApiResult<List<EcInventoryFactorySummaryVO>> factorySummary(
            @RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecInventoryService.listFactorySummary(factoryId));
    }

    /**
     * 获取历史入库价值汇总
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/inbound-value-summary</p>
     *
     * @param factoryId 工厂ID
     * @return 入库价值汇总信息
     */
    @GetMapping("/inbound-value-summary")
    public ApiResult<EcInventoryInboundValueSummaryVO> inboundValueSummary(
            @RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecInventoryService.summarizeHistoricalInboundValue(factoryId));
    }

    /**
     * 装箱估算
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/packing-estimate</p>
     *
     * @param skuCode SKU编码
     * @param outboundQty 出库数量
     * @return 装箱估算结果
     */
    @GetMapping("/packing-estimate")
    public ApiResult<EcInventoryPackingEstimateVO> packingEstimate(
            @RequestParam String skuCode,
            @RequestParam(required = false) Integer outboundQty) {
        return ApiResult.ok(ecInventoryService.estimatePacking(skuCode, outboundQty));
    }

    /**
     * 获取SKU选项列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/sku-options</p>
     *
     * @param factoryId 工厂ID
     * @param productId 商品ID
     * @param productIds 商品ID列表（逗号分隔）
     * @param keyword 关键词
     * @return SKU选项列表
     */
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

    /**
     * 快速入库
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/inventories/inbound</p>
     *
     * @param request 入库请求参数
     * @return 库存详情
     */
    @PostMapping("/inbound")
    public ApiResult<EcInventoryDetailVO> quickInbound(@RequestBody EcInventoryInboundRequest request) {
        return ApiResult.ok(ecInventoryService.quickInbound(request));
    }

    /**
     * 获取可用SKU编码列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/available-sku-codes</p>
     *
     * @return 可用SKU编码列表
     */
    @GetMapping("/available-sku-codes")
    public ApiResult<List<String>> availableSkuCodes() {
        return ApiResult.ok(ecInventoryService.listAvailableSkuCodes());
    }

    /**
     * 获取库存详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/{id}</p>
     *
     * @param id 库存ID
     * @return 库存详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcInventoryDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecInventoryService.getInventoryDetail(id));
    }

    /**
     * 分页查询库存日志
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/inventories/{id}/logs</p>
     *
     * @param id 库存ID
     * @param page 页码
     * @param pageSize 每页条数
     * @return 库存日志分页结果
     */
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

    /**
     * 创建库存
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/inventories</p>
     *
     * @param request 库存保存请求参数
     * @return 创建后的库存信息
     */
    @PostMapping
    public ApiResult<EcInventoryListItemVO> create(@RequestBody EcInventorySaveRequest request) {
        return ApiResult.ok(ecInventoryService.createInventory(request));
    }

    /**
     * 更新库存
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/inventories/{id}</p>
     *
     * @param id 库存ID
     * @param request 库存保存请求参数
     * @return 更新后的库存信息
     */
    @PutMapping("/{id}")
    public ApiResult<EcInventoryListItemVO> update(@PathVariable Long id,
                                                   @RequestBody EcInventorySaveRequest request) {
        return ApiResult.ok(ecInventoryService.updateInventory(id, request));
    }

    /**
     * 调整库存
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/inventories/{id}/adjust</p>
     *
     * @param id 库存ID
     * @param request 库存调整请求参数
     * @return 调整后的库存信息
     */
    @PostMapping("/{id}/adjust")
    public ApiResult<EcInventoryListItemVO> adjust(@PathVariable Long id,
                                                   @RequestBody EcInventoryAdjustRequest request) {
        return ApiResult.ok(ecInventoryService.adjustInventory(id, request));
    }

    /**
     * 删除库存
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/inventories/{id}</p>
     *
     * @param id 库存ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecInventoryService.deleteInventory(id);
        return ApiResult.ok();
    }
}
