package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcInventoryAdjustRequest;
import com.ai.manager.system.domain.dto.EcInventoryInboundRequest;
import com.ai.manager.system.domain.dto.EcInventoryOutboundRequest;
import com.ai.manager.system.domain.dto.EcInventorySaveRequest;
import com.ai.manager.system.domain.entity.EcInventory;
import com.ai.manager.system.domain.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 电商库存服务接口
 *
 * <p>提供电商库存的分页查询、概览统计、详情查看、创建、更新、调整、删除等核心功能，
 * 同时支持库存变动日志查询、工厂库存汇总、包装预估、快速入库、入库、出库、
 * 盘点应用以及SKU可用性校验等库存业务操作。</p>
 */
public interface EcInventoryService extends IService<EcInventory> {

    /**
     * 分页查询库存列表
     *
     * @param keyword     关键词（SKU编码、商品名称等）
     * @param alertOnly   是否仅显示预警库存
     * @param inStockOnly 是否仅显示有库存
     * @param factoryId   工厂ID
     * @param page        页码
     * @param pageSize    每页条数
     * @param groupBySpu  是否按SPU分组
     * @return 库存分页结果
     */
    PageResult<EcInventoryListItemVO> pageInventories(String keyword, Boolean alertOnly, Boolean inStockOnly,
                                                      Long factoryId, Long page, Long pageSize, Boolean groupBySpu);

    /**
     * 获取库存概览统计
     *
     * @return 库存概览数据
     */
    EcInventoryOverviewVO getInventoryOverview();

    /**
     * 获取库存汇总信息
     *
     * @param factoryId 工厂ID
     * @return 库存汇总数据
     */
    EcInventorySummaryVO getInventorySummary(Long factoryId);

    /**
     * 获取SPU状态计数
     *
     * @return SPU各状态数量统计
     */
    EcInventorySpuStatusVO getSpuStatusCounts();

    /**
     * 根据商品ID查询库存列表
     *
     * @param productId 商品ID
     * @return 库存列表
     */
    List<EcInventoryListItemVO> getInventoryByProduct(Long productId);

    /**
     * 获取库存详情
     *
     * @param id 库存ID
     * @return 库存详情信息
     */
    EcInventoryDetailVO getInventoryDetail(Long id);

    /**
     * 创建库存记录
     *
     * @param request 库存保存请求参数
     * @return 创建后的库存信息
     */
    EcInventoryListItemVO createInventory(EcInventorySaveRequest request);

    /**
     * 更新库存记录
     *
     * @param id      库存ID
     * @param request 库存保存请求参数
     * @return 更新后的库存信息
     */
    EcInventoryListItemVO updateInventory(Long id, EcInventorySaveRequest request);

    /**
     * 调整库存数量
     *
     * @param id      库存ID
     * @param request 库存调整请求参数
     * @return 调整后的库存信息
     */
    EcInventoryListItemVO adjustInventory(Long id, EcInventoryAdjustRequest request);

    /**
     * 删除库存记录
     *
     * @param id 库存ID
     */
    void deleteInventory(Long id);

    /**
     * 查询库存变动日志列表
     *
     * @param inventoryId 库存ID
     * @return 库存变动日志列表
     */
    List<EcInventoryLogVO> listLogs(Long inventoryId);

    /**
     * 分页查询库存变动日志
     *
     * @param inventoryId 库存ID
     * @param page        页码
     * @param pageSize    每页条数
     * @return 库存变动日志分页结果
     */
    PageResult<EcInventoryLogVO> pageLogs(Long inventoryId, Long page, Long pageSize);

    /**
     * 分页查询全局库存变动日志
     *
     * @param keyword   关键词
     * @param skuCode   SKU编码
     * @param factoryId 工厂ID
     * @param changeType 变动类型
     * @param refType   关联类型
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      页码
     * @param pageSize  每页条数
     * @return 全局库存变动日志分页结果
     */
    PageResult<EcInventoryGlobalLogVO> pageGlobalLogs(String keyword, String skuCode, Long factoryId,
                                                        String changeType, String refType,
                                                        LocalDateTime startTime, LocalDateTime endTime,
                                                        Long page, Long pageSize);

    /**
     * 查询工厂库存汇总列表
     *
     * @param factoryId 工厂ID
     * @return 工厂库存汇总列表
     */
    List<EcInventoryFactorySummaryVO> listFactorySummary(Long factoryId);

    /**
     * 统计历史入库价值
     *
     * @param factoryId 工厂ID
     * @return 历史入库价值汇总
     */
    EcInventoryInboundValueSummaryVO summarizeHistoricalInboundValue(Long factoryId);

    /**
     * 预估包装信息
     *
     * @param skuCode     SKU编码
     * @param outboundQty 出库数量
     * @return 包装预估结果
     */
    EcInventoryPackingEstimateVO estimatePacking(String skuCode, Integer outboundQty);

    /**
     * 查询可用SKU编码列表
     *
     * @return 可用SKU编码列表
     */
    List<String> listAvailableSkuCodes();

    /**
     * 查询SKU选项列表
     *
     * @param factoryId  工厂ID
     * @param productId  商品ID
     * @param productIds 商品ID列表
     * @param keyword    关键词
     * @return SKU选项列表
     */
    List<EcInventorySkuOptionVO> listSkuOptions(Long factoryId, Long productId, List<Long> productIds, String keyword);

    /**
     * 快速入库
     *
     * @param request 入库请求参数
     * @return 入库后的库存详情
     */
    EcInventoryDetailVO quickInbound(EcInventoryInboundRequest request);

    /**
     * 入库操作
     *
     * @param request 入库请求参数
     * @param refType 关联类型
     * @param refId   关联ID
     * @return 入库后的库存详情
     */
    EcInventoryDetailVO inbound(EcInventoryInboundRequest request, String refType, Long refId);

    /**
     * 出库操作
     *
     * @param request 出库请求参数
     * @param refType 关联类型
     * @param refId   关联ID
     * @return 出库后的库存详情
     */
    EcInventoryDetailVO outbound(EcInventoryOutboundRequest request, String refType, Long refId);

    /**
     * 应用盘点结果
     *
     * @param inventoryId 库存ID
     * @param newQuantity 新数量
     * @param refType     关联类型
     * @param refId       关联ID
     * @param remark      备注
     */
    void applyStocktake(Long inventoryId, int newQuantity, String refType, Long refId, String remark);

    /**
     * 校验SKU是否可入库
     *
     * @param skuCode SKU编码
     */
    void requireSkuAvailableForInbound(String skuCode);
}
