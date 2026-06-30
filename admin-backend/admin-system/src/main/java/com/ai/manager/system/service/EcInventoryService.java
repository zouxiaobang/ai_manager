package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcInventoryAdjustRequest;
import com.ai.manager.system.domain.dto.EcInventoryInboundRequest;
import com.ai.manager.system.domain.dto.EcInventoryOutboundRequest;
import com.ai.manager.system.domain.dto.EcInventorySaveRequest;
import com.ai.manager.system.domain.entity.EcInventory;
import com.ai.manager.system.domain.vo.EcInventoryDetailVO;
import com.ai.manager.system.domain.vo.EcInventoryFactorySummaryVO;
import com.ai.manager.system.domain.vo.EcInventoryGlobalLogVO;
import com.ai.manager.system.domain.vo.EcInventoryListItemVO;
import com.ai.manager.system.domain.vo.EcInventoryLogVO;
import com.ai.manager.system.domain.vo.EcInventoryInboundValueSummaryVO;
import com.ai.manager.system.domain.vo.EcInventoryPackingEstimateVO;
import com.ai.manager.system.domain.vo.EcInventorySkuOptionVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

public interface EcInventoryService extends IService<EcInventory> {

    PageResult<EcInventoryListItemVO> pageInventories(String keyword, Boolean alertOnly, Boolean inStockOnly,
                                                      Long factoryId, Long page, Long pageSize);

    EcInventoryDetailVO getInventoryDetail(Long id);

    EcInventoryListItemVO createInventory(EcInventorySaveRequest request);

    EcInventoryListItemVO updateInventory(Long id, EcInventorySaveRequest request);

    EcInventoryListItemVO adjustInventory(Long id, EcInventoryAdjustRequest request);

    void deleteInventory(Long id);

    List<EcInventoryLogVO> listLogs(Long inventoryId);

    PageResult<EcInventoryLogVO> pageLogs(Long inventoryId, Long page, Long pageSize);

    PageResult<EcInventoryGlobalLogVO> pageGlobalLogs(String keyword, String skuCode, Long factoryId,
                                                        String changeType, String refType,
                                                        LocalDateTime startTime, LocalDateTime endTime,
                                                        Long page, Long pageSize);

    List<EcInventoryFactorySummaryVO> listFactorySummary(Long factoryId);

    EcInventoryInboundValueSummaryVO summarizeHistoricalInboundValue(Long factoryId);

    EcInventoryPackingEstimateVO estimatePacking(String skuCode, Integer outboundQty);

    List<String> listAvailableSkuCodes();

    List<EcInventorySkuOptionVO> listSkuOptions(Long factoryId, Long productId, List<Long> productIds, String keyword);

    EcInventoryDetailVO quickInbound(EcInventoryInboundRequest request);

    EcInventoryDetailVO inbound(EcInventoryInboundRequest request, String refType, Long refId);

    EcInventoryDetailVO outbound(EcInventoryOutboundRequest request, String refType, Long refId);

    void applyStocktake(Long inventoryId, int newQuantity, String refType, Long refId, String remark);

    void requireSkuAvailableForInbound(String skuCode);
}
