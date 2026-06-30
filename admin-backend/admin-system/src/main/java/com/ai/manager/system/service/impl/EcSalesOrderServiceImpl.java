package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcSalesOrderImportManualCostUpdateRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderImportPreviewRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderLineItem;
import com.ai.manager.system.domain.dto.EcSalesOrderLineRefundRequest;
import com.ai.manager.system.domain.dto.EcSalesOrderSaveRequest;
import com.ai.manager.system.domain.enums.EcPlatformCode;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.entity.EcListingLinkSku;
import com.ai.manager.system.domain.entity.EcOrderImportRow;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSalesOrderLine;
import com.ai.manager.system.domain.entity.EcSalesOrderShortage;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.entity.SysImportBatch;
import com.ai.manager.system.domain.entity.SysImportProfile;
import com.ai.manager.system.domain.vo.EcSalesOrderDetailVO;
import com.ai.manager.system.domain.vo.EcSalesOrderImportPreviewVO;
import com.ai.manager.system.domain.vo.EcSalesOrderImportRowVO;
import com.ai.manager.system.domain.vo.EcSalesOrderLineVO;
import com.ai.manager.system.domain.vo.EcSalesOrderMonthlyOverviewVO;
import com.ai.manager.system.domain.vo.EcSalesOrderShortageVO;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.ai.manager.system.mapper.EcOrderImportRowMapper;
import com.ai.manager.system.mapper.EcPlatformMapper;
import com.ai.manager.system.mapper.EcSalesOrderLineMapper;
import com.ai.manager.system.mapper.EcSalesOrderMapper;
import com.ai.manager.system.mapper.EcSalesOrderShortageMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.mapper.SysImportBatchMapper;
import com.ai.manager.system.mapper.SysImportProfileMapper;
import com.ai.manager.system.service.EcSalesOrderImportFileStorage;
import com.ai.manager.system.service.EcSalesOrderService;
import com.ai.manager.system.service.EcSystemSettingsService;
import com.ai.manager.system.service.support.Ec1688ImportLinkNameSupport;
import com.ai.manager.system.service.support.EcAddressProvinceSupport;
import com.ai.manager.system.service.support.EcImportStatusSupport;
import com.ai.manager.system.service.support.EcSalesOrderInventorySupport;
import com.ai.manager.system.service.support.EcSalesOrderMatchSupport;
import com.ai.manager.system.service.support.EcSalesOrderMatchSupport.LinkSkuMatchResult;
import com.ai.manager.system.service.support.EcSalesOrderPricingSupport;
import com.ai.manager.system.service.support.ExpressStationNameAliasSupport;
import com.ai.manager.system.service.support.SysImportFieldRegistry;
import com.ai.manager.system.service.support.SysImportParseSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EcSalesOrderServiceImpl extends ServiceImpl<EcSalesOrderMapper, EcSalesOrder>
        implements EcSalesOrderService {

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_IMPORT = "IMPORT";
    private static final String MANUAL_DEFAULT_PLATFORM_STATUS = "已完成";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PARTIAL_SHIPPED = "PARTIAL_SHIPPED";
    private static final String STATUS_SHIPPED = "SHIPPED";
    private static final String STATUS_PARTIAL_REFUND = "PARTIAL_REFUND";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REFUNDED = "REFUNDED";

    private static final String LINE_PAID = "PAID";
    private static final String LINE_SHIPPED = "SHIPPED";
    private static final String LINE_COMPLETED = "COMPLETED";
    private static final String LINE_CANCELLED = "CANCELLED";
    private static final String LINE_REFUNDED = "REFUNDED";
    private static final String LINE_PARTIAL_REFUND = "PARTIAL_REFUND";
    private static final String LINE_RETURNED = "RETURNED";

    private static final String REFUND_ONLY = "REFUND_ONLY";
    private static final String RETURN_REFUND = "RETURN_REFUND";

    private final EcSalesOrderLineMapper ecSalesOrderLineMapper;
    private final EcSalesOrderShortageMapper ecSalesOrderShortageMapper;
    private final SysImportBatchMapper sysImportBatchMapper;
    private final SysImportProfileMapper sysImportProfileMapper;
    private final EcOrderImportRowMapper ecOrderImportRowMapper;
    private final EcShopMapper ecShopMapper;
    private final EcPlatformMapper ecPlatformMapper;
    private final EcExpressStationMapper ecExpressStationMapper;
    private final EcSalesOrderMatchSupport matchSupport;
    private final EcSalesOrderPricingSupport pricingSupport;
    private final EcSalesOrderInventorySupport inventorySupport;
    private final SysImportParseSupport sysImportParseSupport;
    private final EcSalesOrderImportFileStorage importFileStorage;
    private final ExpressStationNameAliasSupport expressStationNameAliasSupport;
    private final ObjectMapper objectMapper;
    private final EcSystemSettingsService ecSystemSettingsService;

    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public PageResult<EcSalesOrderDetailVO> pageOrders(String keyword, String status, Long shopId,
                                                       String orderTimeFrom, String orderTimeTo,
                                                       Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcSalesOrder> wrapper = new LambdaQueryWrapper<EcSalesOrder>()
                .orderByDesc(EcSalesOrder::getOrderTime)
                .orderByDesc(EcSalesOrder::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(EcSalesOrder::getStatus, status.trim().toUpperCase());
        }
        if (shopId != null) {
            wrapper.eq(EcSalesOrder::getShopId, shopId);
        }
        applyOrderTimeRangeFilter(wrapper, orderTimeFrom, orderTimeTo);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcSalesOrder::getOrderNo, kw)
                    .or().like(EcSalesOrder::getPlatformOrderNo, kw)
                    .or().like(EcSalesOrder::getBuyerName, kw)
                    .or().like(EcSalesOrder::getTrackingNumber, kw));
        }
        Page<EcSalesOrder> entityPage = page(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            return PageUtils.of(List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        }
        Map<Long, EcShop> shopMap = loadShopMap(entityPage.getRecords().stream()
                .map(EcSalesOrder::getShopId).distinct().toList());
        Map<Long, EcPlatform> platformMap = loadPlatformMap(shopMap.values().stream()
                .map(EcShop::getPlatformId).filter(Objects::nonNull).distinct().toList());
        Map<Long, String> stationNameMap = loadStationNameMap(entityPage.getRecords().stream()
                .map(EcSalesOrder::getExpressStationId).filter(Objects::nonNull).distinct().toList());
        List<Long> orderIds = entityPage.getRecords().stream().map(EcSalesOrder::getId).toList();
        Map<Long, Integer> lineCountMap = loadLineCountMap(orderIds);
        Map<Long, EcSalesOrderLine> firstLineMap = loadFirstLineMap(orderIds);
        List<EcSalesOrderDetailVO> records = new ArrayList<>();
        for (EcSalesOrder order : entityPage.getRecords()) {
            int lineCount = lineCountMap.getOrDefault(order.getId(), 0);
            EcSalesOrderDetailVO vo = toDetailVO(order, List.of(), shopMap, platformMap, stationNameMap, lineCount);
            EcSalesOrderLine firstLine = firstLineMap.get(order.getId());
            if (firstLine != null) {
                vo.setLinkName(firstLine.getLinkName());
                vo.setSkuSpecName(firstLine.getSkuSpecName());
            }
            records.add(vo);
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public EcSalesOrderMonthlyOverviewVO getMonthlyOverview(String orderMonth, Long shopId) {
        YearMonth ym = parseOrderMonth(orderMonth);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<EcShop> shops = loadOverviewShops(shopId);
        Map<Long, EcPlatform> platformMap = loadPlatformMap(shops.stream()
                .map(EcShop::getPlatformId).filter(Objects::nonNull).distinct().toList());
        Map<Long, Long> orderCountByShop = countOrdersByShop(start, end, shopId);
        Map<Long, PendingBatchInfo> pendingByShop = loadPendingBatchInfoByShop(ym);

        List<EcSalesOrderMonthlyOverviewVO.ShopImportStatus> shopStatuses = new ArrayList<>();
        int totalOrderCount = 0;
        int importedShopCount = 0;
        int pendingReviewCount = 0;
        LocalDateTime lastImportTime = null;

        for (EcShop shop : shops) {
            int orderCount = orderCountByShop.getOrDefault(shop.getId(), 0L).intValue();
            PendingBatchInfo pending = pendingByShop.get(shop.getId());
            EcSalesOrderMonthlyOverviewVO.ShopImportStatus row = new EcSalesOrderMonthlyOverviewVO.ShopImportStatus();
            row.setShopId(shop.getId());
            row.setShopName(shop.getName());
            row.setShopAvatarUrl(shop.getAvatarUrl());
            if (shop.getPlatformId() != null) {
                EcPlatform platform = platformMap.get(shop.getPlatformId());
                if (platform != null) {
                    row.setPlatformName(platform.getName());
                    row.setPlatformCode(platform.getPlatformCode());
                    row.setPlatformAvatarUrl(platform.getAvatarUrl());
                }
            }
            row.setOrderCount(orderCount);

            if (pending != null) {
                row.setStatus("PENDING_REVIEW");
                row.setPendingBatchId(pending.batchId());
                row.setPendingReviewRows(pending.reviewRows());
                pendingReviewCount += pending.reviewRows();
            } else if (orderCount > 0) {
                row.setStatus("IMPORTED");
            } else {
                row.setStatus("NOT_IMPORTED");
            }

            if (orderCount > 0) {
                importedShopCount++;
            }

            LocalDateTime shopLastImport = loadShopLastImportTime(shop.getId(), start, end);
            if (shopLastImport == null && pending != null && pending.importTime() != null) {
                shopLastImport = pending.importTime();
            }
            row.setLastImportTime(shopLastImport);
            if (shopLastImport != null && (lastImportTime == null || shopLastImport.isAfter(lastImportTime))) {
                lastImportTime = shopLastImport;
            }
            totalOrderCount += orderCount;
            shopStatuses.add(row);
        }

        EcSalesOrderMonthlyOverviewVO vo = new EcSalesOrderMonthlyOverviewVO();
        vo.setOrderMonth(ym.toString());
        vo.setTotalOrderCount(totalOrderCount);
        vo.setImportedShopCount(importedShopCount);
        vo.setTotalShopCount(shops.size());
        vo.setPendingReviewCount(pendingReviewCount);
        vo.setLastImportTime(lastImportTime);
        vo.setShops(shopStatuses);
        return vo;
    }

    private record PendingBatchInfo(Long batchId, int reviewRows, LocalDateTime importTime) {}

    @Override
    public EcSalesOrderDetailVO getOrderDetail(Long id) {
        EcSalesOrder order = requireOrder(id);
        return buildDetailVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderDetailVO createOrder(EcSalesOrderSaveRequest request) {
        EcShop shop = requireShop(request.getShopId());
        String province = resolveReceiveProvince(request);
        List<BuiltLine> lines = buildLines(request.getLines(), shop, request.getExpressStationId(),
                province, false);
        EcSalesOrder order = new EcSalesOrder();
        order.setOrderNo(generateOrderNo());
        order.setShopId(shop.getId());
        order.setPlatformOrderNo(trimToNull(request.getPlatformOrderNo()));
        order.setSource(SOURCE_MANUAL);
        order.setStatus(STATUS_DRAFT);
        order.setActualFreightAmount(BigDecimal.ZERO);
        applyHeaderFields(order, request);
        applyPlatformStatus(order, request);
        save(order);
        saveBuiltLines(order.getId(), lines);
        recalculateOrderTotals(order.getId());
        syncOrderStatus(order.getId());
        return getOrderDetail(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderDetailVO updateOrder(Long id, EcSalesOrderSaveRequest request) {
        EcSalesOrder order = requireEditableOrder(id);
        EcShop shop = requireShop(request.getShopId());
        String province = resolveReceiveProvince(request);
        List<BuiltLine> lines = buildLines(request.getLines(), shop, request.getExpressStationId(),
                province, false);
        order.setShopId(shop.getId());
        order.setPlatformOrderNo(trimToNull(request.getPlatformOrderNo()));
        applyHeaderFields(order, request);
        applyPlatformStatus(order, request);
        updateById(order);
        ecSalesOrderLineMapper.delete(new LambdaQueryWrapper<EcSalesOrderLine>()
                .eq(EcSalesOrderLine::getOrderId, id));
        saveBuiltLines(id, lines);
        recalculateOrderTotals(id);
        syncOrderStatus(id);
        return getOrderDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderDetailVO confirmOrder(Long id) {
        EcSalesOrder order = requireDraftOrder(id);
        List<EcSalesOrderLine> lines = loadLineEntities(id);
        if (lines.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单至少包含一条明细");
        }
        for (EcSalesOrderLine line : lines) {
            if (line.getListingLinkSkuId() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "明细「" + line.getSkuSpecName() + "」未匹配链接 SKU，无法确认");
            }
            line.setStatus(LINE_PAID);
            ecSalesOrderLineMapper.updateById(line);
        }
        order.setStatus(STATUS_PAID);
        if (order.getPayTime() == null) {
            order.setPayTime(LocalDateTime.now());
        }
        updateById(order);
        syncOrderStatus(id);
        return getOrderDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderDetailVO shipLine(Long orderId, Long lineId) {
        EcSalesOrder order = requireOrder(orderId);
        EcSalesOrderLine line = requireLine(orderId, lineId);
        if (!LINE_PAID.equals(line.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅待发货明细可发货");
        }
        shipOneLine(order, line);
        if (order.getShipTime() == null) {
            order.setShipTime(LocalDateTime.now());
            updateById(order);
        }
        recalculateOrderTotals(orderId);
        syncOrderStatus(orderId);
        return getOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderDetailVO shipOrder(Long id) {
        EcSalesOrder order = requireOrder(id);
        List<EcSalesOrderLine> lines = loadLineEntities(id).stream()
                .filter(l -> LINE_PAID.equals(l.getStatus()))
                .toList();
        if (lines.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "没有待发货的明细");
        }
        for (EcSalesOrderLine line : lines) {
            shipOneLine(order, line);
        }
        if (order.getShipTime() == null) {
            order.setShipTime(LocalDateTime.now());
            updateById(order);
        }
        recalculateOrderTotals(id);
        syncOrderStatus(id);
        return getOrderDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderDetailVO refundLine(Long orderId, Long lineId, EcSalesOrderLineRefundRequest request) {
        EcSalesOrder order = requireOrder(orderId);
        EcSalesOrderLine line = requireLine(orderId, lineId);
        String refundType = normalizeRefundType(request != null ? request.getRefundType() : null);
        if (!LINE_SHIPPED.equals(line.getStatus()) && !LINE_COMPLETED.equals(line.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅已发货/已完成明细可退款");
        }
        BigDecimal refundAmount = request != null && request.getRefundAmount() != null
                ? request.getRefundAmount()
                : line.getLineReceivedAmount();
        line.setRefundType(refundType);
        line.setRefundAmount(refundAmount);
        line.setRefundTime(LocalDateTime.now());
        line.setStatus(RETURN_REFUND.equals(refundType) ? LINE_RETURNED : LINE_REFUNDED);
        BigDecimal freightShare = allocateFreight(order, line);
        line.setLossAmount(pricingSupport.calculateLineLoss(line, freightShare));
        line.setProfit(line.getLineReceivedAmount() != null
                ? line.getLineReceivedAmount().subtract(refundAmount != null ? refundAmount : BigDecimal.ZERO)
                .subtract(pricingSupport.lineTotalCost(line)).subtract(freightShare != null ? freightShare : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.subtract(line.getLossAmount()));
        ecSalesOrderLineMapper.updateById(line);
        recalculateOrderTotals(orderId);
        syncOrderStatus(orderId);
        return getOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderDetailVO cancelLine(Long orderId, Long lineId) {
        EcSalesOrder order = requireOrder(orderId);
        EcSalesOrderLine line = requireLine(orderId, lineId);
        if (!LINE_PAID.equals(line.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅待发货明细可取消");
        }
        line.setStatus(LINE_CANCELLED);
        ecSalesOrderLineMapper.updateById(line);
        recalculateOrderTotals(orderId);
        syncOrderStatus(orderId);
        return getOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        EcSalesOrder order = requireDeletableOrder(id);
        ecSalesOrderLineMapper.delete(new LambdaQueryWrapper<EcSalesOrderLine>()
                .eq(EcSalesOrderLine::getOrderId, id));
        ecSalesOrderShortageMapper.delete(new LambdaQueryWrapper<EcSalesOrderShortage>()
                .eq(EcSalesOrderShortage::getOrderId, id));
        removeById(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderImportPreviewVO previewImport(EcSalesOrderImportPreviewRequest request) {
        if (request == null || request.getShopId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择店铺");
        }
        if (request.getRows() == null || request.getRows().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入数据不能为空");
        }
        requireShop(request.getShopId());
        return buildImportPreview(request.getShopId(), request.getProfileId(), request.getFileName(), request.getRows());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderImportPreviewVO uploadImport(MultipartFile file, Long profileId, Long shopId,
                                                    String orderMonth) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请上传文件");
        }
        EcShop shop = requireShop(shopId);
        SysImportProfile profile = resolveImportProfile(profileId, shop);
        if (!SysImportFieldRegistry.BIZ_SALES_ORDER.equals(profile.getBizType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入配置与业务不匹配");
        }
        if (profile.getPlatformId() != null && !Objects.equals(profile.getPlatformId(), shop.getPlatformId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入配置与店铺所属平台不一致");
        }
        try {
            byte[] fileBytes = file.getBytes();
            SysImportParseSupport.ImportParseResult parsed = sysImportParseSupport.parseBytes(
                    fileBytes, file.getOriginalFilename(), profile);
            if (parsed.rows().isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件无有效数据");
            }
            String batchNo = generateBatchNo();
            SysImportBatch batch = new SysImportBatch();
            batch.setBatchNo(batchNo);
            batch.setProfileId(profile.getId());
            batch.setBizType(SysImportFieldRegistry.BIZ_SALES_ORDER);
            batch.setFileName(trimToNull(file.getOriginalFilename()));
            batch.setDetectedColumns(toJson(parsed.headers()));
            batch.setSource("UPLOAD");
            batch.setStatus("PREVIEWED");
            batch.setTotalRows(parsed.rows().size());
            sysImportBatchMapper.insert(batch);
            EcSalesOrderImportFileStorage.SaveResult saved = importFileStorage.save(
                    batchNo, file.getOriginalFilename(), fileBytes);
            batch.setFilePath(saved.panPath());
            batch.setBizContext(buildSalesOrderBatchContext(shopId, orderMonth, saved));
            sysImportBatchMapper.updateById(batch);
            YearMonth ym = parseOrderMonthOptional(orderMonth).orElse(null);
            if (ym == null) {
                ym = resolveBatchOrderMonth(batch).orElse(null);
            }
            if (ym != null) {
                closeOtherPreviewedBatches(shopId, ym, batch.getId());
            }
            EcSalesOrderImportPreviewVO vo = processImportRows(shopId, batch, parsed.rows());
            return enrichImportPreviewVO(vo, batch, shopId);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件解析失败: " + ex.getMessage());
        }
    }

    private EcSalesOrderImportPreviewVO buildImportPreview(Long shopId, Long profileId, String fileName,
                                                           List<Map<String, String>> rows) {
        SysImportBatch batch = new SysImportBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setProfileId(profileId);
        batch.setBizType(SysImportFieldRegistry.BIZ_SALES_ORDER);
        batch.setBizContext(toJson(Map.of("shopId", shopId)));
        batch.setFileName(trimToNull(fileName));
        batch.setSource("UPLOAD");
        batch.setStatus("PREVIEWED");
        batch.setTotalRows(rows.size());
        sysImportBatchMapper.insert(batch);
        return processImportRows(shopId, batch, rows);
    }

    private EcSalesOrderImportPreviewVO processImportRows(Long shopId, SysImportBatch batch,
                                                          List<Map<String, String>> rows) {
        EcShop shop = requireShop(shopId);
        boolean parse1688LinkSku = is1688Platform(shop.getPlatformId());
        SysImportProfile profile = batch.getProfileId() != null
                ? sysImportProfileMapper.selectById(batch.getProfileId()) : null;
        EcImportStatusSupport statusSupport = createStatusSupport(profile);
        Set<String> sellerRemarkOrders = parse1688LinkSku
                ? collect1688OrdersWithSellerRemark(rows) : Set.of();
        int matched = 0;
        int unmatched = 0;
        int statusUnmatched = 0;
        int errors = 0;
        List<EcSalesOrderImportRowVO> previewRows = new ArrayList<>();
        int rowNo = 0;
        for (Map<String, String> raw : rows) {
            rowNo++;
            EcOrderImportRow entity = new EcOrderImportRow();
            entity.setBatchId(batch.getId());
            entity.setRowNo(rowNo);
            entity.setPlatformOrderNo(getMapValue(raw, "platform_order_no", "platformOrderNo"));
            if (parse1688LinkSku) {
                apply1688LinkSkuParsing(raw);
            }
            entity.setLinkName(getMapValue(raw, "link_name", "linkName"));
            entity.setSkuSpecName(getMapValue(raw, "sku_spec_name", "skuSpecName"));
            entity.setRawJson(toJson(raw));
            entity.setManualCostPrice(parseDecimal(getMapValue(raw, "manual_cost_price", "manualCostPrice")));
            applyImportRowStatus(entity, raw, statusSupport);
            if ("UNMATCHED".equals(entity.getStatusMatchStatus())) {
                statusUnmatched++;
            }
            try {
                if (!StringUtils.hasText(entity.getLinkName())) {
                    entity.setParseStatus("ERROR");
                    entity.setErrorMessage("链接名称不能为空");
                    errors++;
                } else {
                    LinkSkuMatchResult match = matchSupport.matchLinkSku(
                            shopId, entity.getLinkName(), entity.getSkuSpecName());
                    if (match.isMatched()) {
                        entity.setParseStatus("OK");
                        entity.setMatchStatus("MATCHED");
                        entity.setListingLinkSkuId(match.getListingLinkSkuId());
                        if (StringUtils.hasText(match.getSkuSpecName())) {
                            entity.setSkuSpecName(match.getSkuSpecName());
                        }
                        matched++;
                    } else {
                        entity.setParseStatus("OK");
                        entity.setMatchStatus("UNMATCHED");
                        entity.setErrorMessage(mergeImportRowMessages(match.getMessage(), entity.getErrorMessage()));
                        unmatched++;
                    }
                }
            } catch (Exception ex) {
                entity.setParseStatus("ERROR");
                entity.setErrorMessage(ex.getMessage());
                errors++;
            }
            if (parse1688LinkSku && requiresManualCostForSellerRemark(entity, raw, sellerRemarkOrders)) {
                if ("MATCHED".equals(entity.getMatchStatus())) {
                    matched--;
                    unmatched++;
                }
                entity.setMatchStatus("UNMATCHED");
                entity.setListingLinkSkuId(null);
                entity.setErrorMessage(mergeImportRowMessages(
                        "订单有卖家备注，请手动填写成本", entity.getErrorMessage()));
            }
            ecOrderImportRowMapper.insert(entity);
            EcSalesOrderImportRowVO rowVo = toImportRowVO(entity);
            previewRows.add(rowVo);
        }
        batch.setSuccessRows(0);
        batch.setFailedRows(errors);
        batch.setUnmatchedRows(unmatched);
        sysImportBatchMapper.updateById(batch);

        EcSalesOrderImportPreviewVO vo = new EcSalesOrderImportPreviewVO();
        vo.setBatchId(batch.getId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setTotalRows(batch.getTotalRows());
        vo.setMatchedRows(matched);
        vo.setUnmatchedRows(unmatched);
        vo.setStatusUnmatchedRows(statusUnmatched);
        vo.setErrorRows(errors);
        vo.setRows(previewRows);
        return vo;
    }

    private void applyImportRowStatus(EcOrderImportRow entity, Map<String, String> raw,
                                      EcImportStatusSupport statusSupport) {
        String platformLineStatus = readImportPlatformStatus(raw);
        entity.setPlatformLineStatus(StringUtils.hasText(platformLineStatus) ? platformLineStatus.trim() : null);
        EcImportStatusSupport.ResolveResult resolved = statusSupport.resolveDetailed(platformLineStatus);
        if (!resolved.matched()) {
            String inferred = inferLineStatusFromRaw(raw);
            if (inferred != null) {
                resolved = new EcImportStatusSupport.ResolveResult(true, inferred, platformLineStatus);
            }
        }
        if (resolved.matched()) {
            entity.setStatusMatchStatus("MATCHED");
            entity.setLineStatus(resolved.lineStatus());
        } else {
            entity.setStatusMatchStatus("UNMATCHED");
            entity.setLineStatus(null);
            String message = StringUtils.hasText(resolved.platformText())
                    ? "平台状态「" + resolved.platformText() + "」未映射，请选择系统状态"
                    : "未识别到平台订单状态，请手动选择系统状态";
            entity.setErrorMessage(mergeImportRowMessages(message, entity.getErrorMessage()));
        }
    }

    private String readImportPlatformStatus(Map<String, String> raw) {
        String platformLineStatus = getMapValue(raw, "platform_line_status", "platformLineStatus");
        if (!StringUtils.hasText(platformLineStatus)) {
            platformLineStatus = getMapValue(raw, "platform_status", "platformStatus");
        }
        return platformLineStatus;
    }

    private SysImportProfile resolveImportProfile(SysImportBatch batch) {
        return batch.getProfileId() != null ? sysImportProfileMapper.selectById(batch.getProfileId()) : null;
    }

    /**
     * 恢复预览或入库前，从 platform_line_status / raw_json 回填缺失或错误的 line_status。
     * 人工指定且 status_match_status=UNMATCHED 的行不覆盖。
     */
    private void hydrateImportRowLineStatus(EcOrderImportRow row, EcImportStatusSupport statusSupport) {
        if ("UNMATCHED".equals(row.getStatusMatchStatus()) && StringUtils.hasText(row.getLineStatus())) {
            return;
        }
        String platformStatus = readImportPlatformStatusFromRow(row);
        Map<String, String> raw = readImportRowRawAsStringMap(row);
        EcImportStatusSupport.ResolveResult resolved = statusSupport.resolveDetailed(platformStatus);
        if (!resolved.matched()) {
            String inferred = inferLineStatusFromRaw(raw);
            if (inferred != null) {
                resolved = new EcImportStatusSupport.ResolveResult(true, inferred, platformStatus);
            }
        }
        if (!resolved.matched()) {
            return;
        }
        String expected = resolved.lineStatus();
        boolean statusBlank = !StringUtils.hasText(row.getLineStatus());
        boolean statusMismatch = !statusBlank
                && !expected.equalsIgnoreCase(row.getLineStatus().trim());
        boolean platformBlank = !StringUtils.hasText(row.getPlatformLineStatus());
        if (!statusBlank && !statusMismatch && !platformBlank) {
            return;
        }
        row.setLineStatus(expected);
        row.setStatusMatchStatus("MATCHED");
        if (StringUtils.hasText(resolved.platformText())) {
            row.setPlatformLineStatus(resolved.platformText());
        } else if (StringUtils.hasText(platformStatus)) {
            row.setPlatformLineStatus(platformStatus);
        }
        ecOrderImportRowMapper.updateById(row);
    }

    private String readImportPlatformStatusFromRow(EcOrderImportRow row) {
        if (StringUtils.hasText(row.getPlatformLineStatus())) {
            return row.getPlatformLineStatus().trim();
        }
        Map<String, String> raw = readImportRowRawAsStringMap(row);
        return readImportPlatformStatus(raw);
    }

    private Map<String, String> readImportRowRawAsStringMap(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null && StringUtils.hasText(String.valueOf(entry.getValue()))) {
                    result.put(entry.getKey(), String.valueOf(entry.getValue()).trim());
                }
            }
            return result;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /** 平台状态列缺失时，根据时间/物流字段推断行状态 */
    private String inferLineStatusFromRaw(Map<String, String> raw) {
        if (StringUtils.hasText(getMapValue(raw, "complete_time", "completeTime"))) {
            return "COMPLETED";
        }
        if (StringUtils.hasText(getMapValue(raw, "ship_time", "shipTime"))
                || StringUtils.hasText(getMapValue(raw, "tracking_number", "trackingNumber"))) {
            return "SHIPPED";
        }
        if (StringUtils.hasText(getMapValue(raw, "pay_time", "payTime"))) {
            return "PAID";
        }
        return null;
    }

    private String mergeImportRowMessages(String primary, String secondary) {
        if (!StringUtils.hasText(primary)) {
            return secondary;
        }
        if (!StringUtils.hasText(secondary)) {
            return primary;
        }
        if (primary.contains(secondary) || secondary.contains(primary)) {
            return primary.length() >= secondary.length() ? primary : secondary;
        }
        return primary + "；" + secondary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderImportPreviewVO updateImportManualCosts(Long batchId,
                                                               EcSalesOrderImportManualCostUpdateRequest request) {
        SysImportBatch batch = requireImportBatchForEdit(batchId);
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请提供要更新的行");
        }
        applyImportManualCosts(batchId, request, Set.of());
        EcSalesOrderImportPreviewVO vo = buildImportPreviewVO(batch);
        return enrichImportPreviewVO(vo, batch, readShopIdFromBatch(batch));
    }

    @Override
    public EcSalesOrderImportPreviewVO getImportPreview(Long batchId) {
        SysImportBatch batch = requireImportBatchForEdit(batchId);
        if (!SysImportFieldRegistry.BIZ_SALES_ORDER.equals(batch.getBizType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "批次类型不匹配");
        }
        EcSalesOrderImportPreviewVO vo = buildImportPreviewVO(batch);
        return enrichImportPreviewVO(vo, batch, readShopIdFromBatch(batch));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderImportPreviewVO reparseImport(Long batchId) {
        SysImportBatch batch = requireImportBatchForEdit(batchId);
        if (!SysImportFieldRegistry.BIZ_SALES_ORDER.equals(batch.getBizType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "批次类型不匹配");
        }
        return reparseExistingBatch(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderImportPreviewVO replaceImportFile(Long batchId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请上传文件");
        }
        SysImportBatch batch = requireImportBatchForEdit(batchId);
        if (!SysImportFieldRegistry.BIZ_SALES_ORDER.equals(batch.getBizType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "批次类型不匹配");
        }
        try {
            byte[] fileBytes = file.getBytes();
            EcSalesOrderImportFileStorage.SaveResult saved = importFileStorage.save(
                    batch.getBatchNo(), file.getOriginalFilename(), fileBytes);
            batch.setFileName(trimToNull(file.getOriginalFilename()));
            batch.setFilePath(saved.panPath());
            batch.setBizContext(buildSalesOrderBatchContext(readShopIdFromBatch(batch),
                    readOrderMonthFromBatch(batch).map(YearMonth::toString).orElse(null), saved));
            sysImportBatchMapper.updateById(batch);
            return reparseExistingBatch(batch);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件替换失败: " + ex.getMessage());
        }
    }

    private EcSalesOrderImportPreviewVO reparseExistingBatch(SysImportBatch batch) {
        Long shopId = readShopIdFromBatch(batch);
        EcShop shop = requireShop(shopId);
        SysImportProfile profile = resolveImportProfile(batch.getProfileId(), shop);
        try {
            byte[] bytes = importFileStorage.load(batch);
            if (bytes == null || bytes.length == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入文件不存在，请重新上传");
            }
            SysImportParseSupport.ImportParseResult parsed = sysImportParseSupport.parseBytes(
                    bytes, batch.getFileName(), profile);
            if (parsed.rows().isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件无有效数据");
            }
            clearImportRows(batch.getId());
            batch.setTotalRows(parsed.rows().size());
            batch.setDetectedColumns(toJson(parsed.headers()));
            batch.setStatus("PREVIEWED");
            batch.setSuccessRows(0);
            batch.setFailedRows(0);
            batch.setUnmatchedRows(0);
            sysImportBatchMapper.updateById(batch);
            EcSalesOrderImportPreviewVO vo = processImportRows(shopId, batch, parsed.rows());
            return enrichImportPreviewVO(vo, batch, shopId);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "重新解析失败: " + ex.getMessage());
        }
    }

    private void clearImportRows(Long batchId) {
        ecOrderImportRowMapper.delete(new LambdaQueryWrapper<EcOrderImportRow>()
                .eq(EcOrderImportRow::getBatchId, batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSalesOrderImportPreviewVO commitImport(Long batchId,
                                                      EcSalesOrderImportManualCostUpdateRequest request) {
        SysImportBatch batch = requireImportBatchForEdit(batchId);
        Set<String> excludedLineStatuses = resolveExcludedLineStatuses(request);
        if (request != null && request.getItems() != null && !request.getItems().isEmpty()) {
            applyImportManualCosts(batchId, request, excludedLineStatuses);
        }
        Long shopId = readShopIdFromBatch(batch);
        EcShop shop = requireShop(shopId);
        List<EcOrderImportRow> rows = ecOrderImportRowMapper.selectList(new LambdaQueryWrapper<EcOrderImportRow>()
                .eq(EcOrderImportRow::getBatchId, batchId)
                .orderByAsc(EcOrderImportRow::getRowNo));
        SysImportProfile profile = resolveImportProfile(batch);
        EcImportStatusSupport statusSupport = createStatusSupport(profile);
        for (EcOrderImportRow row : rows) {
            if ("OK".equals(row.getParseStatus())) {
                hydrateImportRowLineStatus(row, statusSupport);
            }
        }
        for (EcOrderImportRow row : rows) {
            if (!"OK".equals(row.getParseStatus())) {
                continue;
            }
            if (isExcludedImportRow(row, excludedLineStatuses) && "UNMATCHED".equals(row.getMatchStatus())) {
                row.setManualCostPrice(BigDecimal.ZERO);
                ecOrderImportRowMapper.updateById(row);
            }
        }
        List<Integer> missingCostRows = new ArrayList<>();
        List<Integer> missingStatusRows = new ArrayList<>();
        Map<String, List<EcOrderImportRow>> grouped = new LinkedHashMap<>();
        for (EcOrderImportRow row : rows) {
            if (!"OK".equals(row.getParseStatus())) {
                continue;
            }
            if ("UNMATCHED".equals(row.getStatusMatchStatus())
                    && !StringUtils.hasText(row.getLineStatus())) {
                missingStatusRows.add(row.getRowNo());
                continue;
            }
            if ("UNMATCHED".equals(row.getMatchStatus())) {
                if (row.getManualCostPrice() == null
                        || row.getManualCostPrice().compareTo(BigDecimal.ZERO) == 0) {
                    if (!isExcludedImportRow(row, excludedLineStatuses)) {
                        missingCostRows.add(row.getRowNo());
                    }
                    continue;
                }
            }
            addImportRowToGroup(grouped, row);
        }
        if (!missingStatusRows.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "状态未映射行请先选择系统状态，行号：" + missingStatusRows.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")));
        }
        if (!missingCostRows.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "未匹配行请先填写手动成本，行号：" + missingCostRows.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")));
        }
        if (grouped.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "没有可入库的有效行");
        }
        int success = 0;
        for (Map.Entry<String, List<EcOrderImportRow>> entry : grouped.entrySet()) {
            String platformOrderNo = entry.getKey().startsWith("ROW-") ? null : entry.getKey();
            if (platformOrderNo != null) {
                EcSalesOrder existing = getOne(new LambdaQueryWrapper<EcSalesOrder>()
                        .eq(EcSalesOrder::getShopId, shop.getId())
                        .eq(EcSalesOrder::getPlatformOrderNo, platformOrderNo)
                        .last("LIMIT 1"));
                if (existing != null) {
                    replaceOrderFromImportRows(existing, batch, entry.getValue(), shop, statusSupport);
                    success += entry.getValue().size();
                    continue;
                }
            }
            EcSalesOrder order = createOrderFromImportRows(shop, batch, platformOrderNo, entry.getValue(), statusSupport);
            success += entry.getValue().size();
            for (EcOrderImportRow row : entry.getValue()) {
                row.setSalesOrderId(order.getId());
                ecOrderImportRowMapper.updateById(row);
            }
        }
        batch.setStatus("COMMITTED");
        batch.setSuccessRows(success);
        batch.setCommittedTime(LocalDateTime.now());
        sysImportBatchMapper.updateById(batch);
        YearMonth batchMonth = resolveBatchOrderMonth(batch).orElse(null);
        if (batchMonth != null) {
            closeOtherPreviewedBatches(shopId, batchMonth, batch.getId());
        }

        return buildImportPreviewVO(batch, success);
    }

    private void addImportRowToGroup(Map<String, List<EcOrderImportRow>> grouped, EcOrderImportRow row) {
        String key = StringUtils.hasText(row.getPlatformOrderNo())
                ? row.getPlatformOrderNo().trim()
                : "ROW-" + row.getRowNo();
        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
    }

    private SysImportBatch requireImportBatchForEdit(Long batchId) {
        SysImportBatch batch = sysImportBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if ("COMMITTED".equals(batch.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该批次已入库");
        }
        return batch;
    }

    private void applyImportManualCosts(Long batchId,
                                        EcSalesOrderImportManualCostUpdateRequest request,
                                        Set<String> excludedLineStatuses) {
        for (EcSalesOrderImportManualCostUpdateRequest.Item item : request.getItems()) {
            if (item.getRowId() == null) {
                continue;
            }
            EcOrderImportRow row = ecOrderImportRowMapper.selectById(item.getRowId());
            if (row == null || !Objects.equals(row.getBatchId(), batchId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入行不存在或不属于当前批次");
            }
            if (item.getManualCostPrice() != null) {
                if (!"UNMATCHED".equals(row.getMatchStatus())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅未匹配链接行可填写手动成本");
                }
                if (item.getManualCostPrice().compareTo(BigDecimal.ZERO) == 0
                        && !isExcludedImportRow(row, excludedLineStatuses)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "手动成本不能为 0");
                }
                row.setManualCostPrice(item.getManualCostPrice());
            }
            if (StringUtils.hasText(item.getLineStatus())) {
                row.setLineStatus(normalizeImportLineStatus(item.getLineStatus()));
                row.setStatusMatchStatus("MATCHED");
            }
            ecOrderImportRowMapper.updateById(row);
        }
    }

    private Set<String> resolveExcludedLineStatuses(EcSalesOrderImportManualCostUpdateRequest request) {
        if (request == null || request.getExcludedLineStatuses() == null
                || request.getExcludedLineStatuses().isEmpty()) {
            return Set.of();
        }
        Set<String> excluded = new LinkedHashSet<>();
        for (String status : request.getExcludedLineStatuses()) {
            if (StringUtils.hasText(status)) {
                excluded.add(normalizeImportLineStatus(status));
            }
        }
        return excluded;
    }

    private boolean isExcludedImportRow(EcOrderImportRow row, Set<String> excludedLineStatuses) {
        if (excludedLineStatuses.isEmpty() || !StringUtils.hasText(row.getLineStatus())) {
            return false;
        }
        return excludedLineStatuses.contains(row.getLineStatus().trim().toUpperCase());
    }

    private static String normalizeImportLineStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("PAID", "SHIPPED", "COMPLETED", "CANCELLED", "PARTIAL_REFUND", "REFUNDED", "RETURNED")
                .contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "无效的行状态: " + status);
        }
        return normalized;
    }

    private EcSalesOrderImportPreviewVO buildImportPreviewVO(SysImportBatch batch) {
        List<EcOrderImportRow> rows = ecOrderImportRowMapper.selectList(new LambdaQueryWrapper<EcOrderImportRow>()
                .eq(EcOrderImportRow::getBatchId, batch.getId())
                .orderByAsc(EcOrderImportRow::getRowNo));
        EcImportStatusSupport statusSupport = createStatusSupport(resolveImportProfile(batch));
        int matched = 0;
        int unmatched = 0;
        int statusUnmatched = 0;
        int errors = 0;
        List<EcSalesOrderImportRowVO> previewRows = new ArrayList<>();
        for (EcOrderImportRow row : rows) {
            hydrateImportRowLineStatus(row, statusSupport);
            if ("ERROR".equals(row.getParseStatus())) {
                errors++;
            } else if ("MATCHED".equals(row.getMatchStatus())) {
                matched++;
            } else if ("UNMATCHED".equals(row.getMatchStatus())) {
                unmatched++;
            }
            if ("UNMATCHED".equals(row.getStatusMatchStatus())) {
                statusUnmatched++;
            }
            previewRows.add(toImportRowVO(row));
        }
        EcSalesOrderImportPreviewVO vo = new EcSalesOrderImportPreviewVO();
        vo.setBatchId(batch.getId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setTotalRows(batch.getTotalRows());
        vo.setMatchedRows(matched);
        vo.setUnmatchedRows(unmatched);
        vo.setStatusUnmatchedRows(statusUnmatched);
        vo.setErrorRows(errors);
        vo.setRows(previewRows);
        return vo;
    }

    private EcSalesOrderImportPreviewVO buildImportPreviewVO(SysImportBatch batch, int committedRows) {
        EcSalesOrderImportPreviewVO vo = buildImportPreviewVO(batch);
        vo.setMatchedRows(committedRows);
        return vo;
    }

    // ---- internal helpers ----

    private List<BuiltLine> buildLines(List<EcSalesOrderLineItem> items, EcShop shop,
                                       Long expressStationId, String province, boolean requireMatch) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请至少添加一条订单明细");
        }
        List<BuiltLine> result = new ArrayList<>();
        int sort = 0;
        for (EcSalesOrderLineItem item : items) {
            if (item.getSortOrder() == null) {
                item.setSortOrder(sort++);
            }
            result.add(buildSingleLine(item, shop, expressStationId, province, requireMatch));
        }
        return result;
    }

    private BuiltLine buildSingleLine(EcSalesOrderLineItem item, EcShop shop,
                                      Long expressStationId, String province, boolean requireMatch) {
        LinkSkuMatchResult match;
        if (item.getListingLinkSkuId() != null) {
            match = matchSupport.matchByListingLinkSkuId(item.getListingLinkSkuId());
        } else {
            match = matchSupport.matchLinkSku(shop.getId(), item.getLinkName(), item.getSkuSpecName());
        }
        if (!match.isMatched()) {
            if (requireMatch) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), match.getMessage());
            }
        }
        EcSalesOrderLine line = new EcSalesOrderLine();
        line.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
        line.setSkuQuantity(item.getSkuQuantity() != null && item.getSkuQuantity() > 0 ? item.getSkuQuantity() : 1);
        line.setShippedQuantity(0);
        line.setShortQuantity(0);
        line.setStatus(LINE_PAID);
        line.setUnitPrice(item.getUnitPrice());
        line.setLineReceivedAmount(item.getLineReceivedAmount());
        line.setLineCouponAmount(item.getLineCouponAmount());
        line.setPlatformLineNo(trimToNull(item.getPlatformLineNo()));
        line.setPlatformItemName(trimToNull(item.getPlatformItemName()));
        line.setPlatformLineStatus(trimToNull(item.getPlatformLineStatus()));
        if (match.isMatched()) {
            line.setListingLinkSkuId(match.getListingLinkSkuId());
            line.setLinkName(match.getLinkName());
            line.setSkuSpecName(match.getSkuSpecName());
            line.setSkuCodes(match.getSkuCodes());
            EcListingLinkSku linkSku = match.getListingLinkSku();
            String prov = EcAddressProvinceSupport.resolveFreightProvince(province, shop.getDefaultReceiveProvince());
            pricingSupport.applyLinePricing(line, linkSku, shop, prov, expressStationId);
        } else {
            line.setLinkName(trimToNull(item.getLinkName()));
            line.setSkuSpecName(trimToNull(item.getSkuSpecName()));
            if (item.getManualCostPrice() != null) {
                applyManualLinePricing(line, item.getManualCostPrice(), item.getLineReceivedAmount());
            }
        }
        BuiltLine built = new BuiltLine();
        built.entity = line;
        return built;
    }

    private void saveBuiltLines(Long orderId, List<BuiltLine> lines) {
        for (BuiltLine built : lines) {
            built.entity.setOrderId(orderId);
            ecSalesOrderLineMapper.insert(built.entity);
        }
    }

    private void applyHeaderFields(EcSalesOrder order, EcSalesOrderSaveRequest request) {
        order.setExpressStationId(request.getExpressStationId());
        LocalDateTime orderTime = request.getOrderTime() != null ? request.getOrderTime() : LocalDateTime.now();
        order.setOrderTime(orderTime);
        order.setPayTime(request.getPayTime() != null ? request.getPayTime() : orderTime);
        order.setBuyerName(trimToNull(request.getBuyerName()));
        order.setBuyerPhone(trimToNull(request.getBuyerPhone()));
        order.setReceiveCity(trimToNull(request.getReceiveCity()));
        order.setReceiveDistrict(trimToNull(request.getReceiveDistrict()));
        order.setReceiveAddress(trimToNull(request.getReceiveAddress()));
        order.setReceiveProvince(resolveReceiveProvince(request));
        order.setTrackingNumber(trimToNull(request.getTrackingNumber()));
        order.setBuyerRemark(trimToNull(request.getBuyerRemark()));
        order.setSellerRemark(trimToNull(request.getSellerRemark()));
        order.setReceivedAmount(request.getReceivedAmount());
        order.setFreightAmount(request.getFreightAmount());
        if (order.getActualFreightAmount() == null) {
            order.setActualFreightAmount(BigDecimal.ZERO);
        }
        order.setOrderCouponAmount(request.getOrderCouponAmount());
        order.setHasShortage(0);
    }

    private String resolveReceiveProvince(EcSalesOrderSaveRequest request) {
        if (StringUtils.hasText(request.getReceiveProvince())) {
            return trimToNull(request.getReceiveProvince());
        }
        return EcAddressProvinceSupport.parseProvince(trimToNull(request.getReceiveAddress()));
    }

    private void applyPlatformStatus(EcSalesOrder order, EcSalesOrderSaveRequest request) {
        if (StringUtils.hasText(request.getPlatformStatus())) {
            order.setPlatformStatus(trimToNull(request.getPlatformStatus()));
            return;
        }
        if (SOURCE_MANUAL.equals(order.getSource()) && !StringUtils.hasText(order.getPlatformStatus())) {
            order.setPlatformStatus(MANUAL_DEFAULT_PLATFORM_STATUS);
        }
    }

    private String resolvePlatformStatus(EcSalesOrder order) {
        if (StringUtils.hasText(order.getPlatformStatus())) {
            return order.getPlatformStatus().trim();
        }
        if (SOURCE_MANUAL.equals(order.getSource())) {
            return MANUAL_DEFAULT_PLATFORM_STATUS;
        }
        return null;
    }

    private void recalculateOrderTotals(Long orderId) {
        List<EcSalesOrderLine> lines = loadLineEntities(orderId);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal platformFee = BigDecimal.ZERO;
        for (EcSalesOrderLine line : lines) {
            if (LINE_CANCELLED.equals(line.getStatus())) {
                continue;
            }
            totalCost = totalCost.add(pricingSupport.lineTotalCost(line));
            if (line.getProfit() != null) {
                totalProfit = totalProfit.add(line.getProfit());
            }
            if (line.getLossAmount() != null) {
                totalLoss = totalLoss.add(line.getLossAmount());
            }
            if (line.getLineReceivedAmount() != null) {
                received = received.add(line.getLineReceivedAmount());
            }
            if (line.getPlatformFeeAmount() != null && line.getSkuQuantity() != null) {
                platformFee = platformFee.add(line.getPlatformFeeAmount()
                        .multiply(new BigDecimal(line.getSkuQuantity())));
            }
        }
        EcSalesOrder order = requireOrder(orderId);
        EcShop shop = requireShop(order.getShopId());
        order.setTotalCostAmount(totalCost.setScale(2, RoundingMode.HALF_UP));
        order.setProfitAmount(totalProfit.setScale(2, RoundingMode.HALF_UP));
        order.setTotalLossAmount(totalLoss.setScale(2, RoundingMode.HALF_UP));
        if (order.getReceivedAmount() == null && received.compareTo(BigDecimal.ZERO) > 0) {
            order.setReceivedAmount(received.setScale(2, RoundingMode.HALF_UP));
        }
        order.setPlatformFeeAmount(platformFee.setScale(2, RoundingMode.HALF_UP));
        order.setEstimatedFreightAmount(pricingSupport.calculateOrderEstimatedFreight(order, shop, lines));
        updateById(order);
    }

    private void syncOrderStatus(Long orderId) {
        List<EcSalesOrderLine> lines = loadLineEntities(orderId);
        EcSalesOrder order = requireOrder(orderId);
        if (lines.isEmpty()) {
            return;
        }
        if (STATUS_DRAFT.equals(order.getStatus())) {
            return;
        }
        long paid = lines.stream().filter(l -> LINE_PAID.equals(l.getStatus())).count();
        long shipped = lines.stream().filter(l -> LINE_SHIPPED.equals(l.getStatus())).count();
        long completed = lines.stream().filter(l -> LINE_COMPLETED.equals(l.getStatus())).count();
        long cancelled = lines.stream().filter(l -> LINE_CANCELLED.equals(l.getStatus())).count();
        long fullRefunded = lines.stream().filter(l -> LINE_REFUNDED.equals(l.getStatus())
                || LINE_RETURNED.equals(l.getStatus())).count();
        long partialRefunded = lines.stream().filter(l -> LINE_PARTIAL_REFUND.equals(l.getStatus())).count();
        long active = lines.size() - cancelled;

        String newStatus;
        if (partialRefunded > 0) {
            newStatus = STATUS_PARTIAL_REFUND;
        } else if (fullRefunded > 0 && fullRefunded >= active) {
            newStatus = STATUS_REFUNDED;
        } else if (cancelled == lines.size()) {
            newStatus = STATUS_CANCELLED;
        } else if (fullRefunded > 0) {
            newStatus = STATUS_PARTIAL_REFUND;
        } else if (completed > 0 && completed + shipped >= active) {
            newStatus = STATUS_COMPLETED;
        } else if (shipped > 0 && shipped + completed < active) {
            newStatus = STATUS_PARTIAL_SHIPPED;
        } else if (shipped > 0 || completed > 0) {
            newStatus = STATUS_SHIPPED;
        } else if (paid > 0) {
            newStatus = STATUS_PAID;
        } else {
            newStatus = order.getStatus();
        }
        if (!newStatus.equals(order.getStatus())) {
            order.setStatus(newStatus);
            updateById(order);
        }
    }

    private BigDecimal allocateFreight(EcSalesOrder order, EcSalesOrderLine line) {
        if (order.getActualFreightAmount() == null
                || order.getActualFreightAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        List<EcSalesOrderLine> lines = loadLineEntities(order.getId()).stream()
                .filter(l -> !LINE_CANCELLED.equals(l.getStatus()))
                .toList();
        if (lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal perLine = order.getActualFreightAmount()
                .divide(new BigDecimal(lines.size()), 2, RoundingMode.HALF_UP);
        return perLine;
    }

    private String normalizeRefundType(String refundType) {
        if (!StringUtils.hasText(refundType)) {
            return REFUND_ONLY;
        }
        String t = refundType.trim().toUpperCase();
        if (RETURN_REFUND.equals(t)) {
            return RETURN_REFUND;
        }
        return REFUND_ONLY;
    }

    private EcSalesOrderDetailVO buildDetailVO(EcSalesOrder order) {
        Map<Long, EcShop> shopMap = loadShopMap(List.of(order.getShopId()));
        Map<Long, EcPlatform> platformMap = loadPlatformMap(shopMap.values().stream()
                .map(EcShop::getPlatformId).filter(Objects::nonNull).distinct().toList());
        Map<Long, String> stationNameMap = loadStationNameMap(order.getExpressStationId() != null
                ? List.of(order.getExpressStationId()) : List.of());
        List<EcSalesOrderLineVO> lines = loadLineVOs(order.getId());
        return toDetailVO(order, lines, shopMap, platformMap, stationNameMap, lines.size());
    }

    private List<EcSalesOrderLine> loadLineEntities(Long orderId) {
        return ecSalesOrderLineMapper.selectList(new LambdaQueryWrapper<EcSalesOrderLine>()
                .eq(EcSalesOrderLine::getOrderId, orderId)
                .orderByAsc(EcSalesOrderLine::getSortOrder)
                .orderByAsc(EcSalesOrderLine::getId));
    }

    private List<EcSalesOrderLineVO> loadLineVOs(Long orderId) {
        List<EcSalesOrderLine> lines = loadLineEntities(orderId);
        Map<Long, List<EcSalesOrderShortage>> shortageMap = ecSalesOrderShortageMapper.selectList(
                        new LambdaQueryWrapper<EcSalesOrderShortage>().eq(EcSalesOrderShortage::getOrderId, orderId))
                .stream().collect(Collectors.groupingBy(EcSalesOrderShortage::getOrderLineId));
        List<EcSalesOrderLineVO> result = new ArrayList<>();
        for (EcSalesOrderLine line : lines) {
            EcSalesOrderLineVO vo = toLineVO(line);
            List<EcSalesOrderShortage> shortages = shortageMap.getOrDefault(line.getId(), List.of());
            vo.setShortages(shortages.stream().map(this::toShortageVO).toList());
            result.add(vo);
        }
        return result;
    }

    private EcSalesOrderDetailVO toDetailVO(EcSalesOrder order, List<EcSalesOrderLineVO> lines,
                                            Map<Long, EcShop> shopMap, Map<Long, EcPlatform> platformMap,
                                            Map<Long, String> stationNameMap, int lineCount) {
        EcSalesOrderDetailVO vo = new EcSalesOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setShopId(order.getShopId());
        EcShop shop = shopMap.get(order.getShopId());
        if (shop != null) {
            vo.setShopName(shop.getName());
            if (shop.getPlatformId() != null) {
                vo.setPlatformId(shop.getPlatformId());
                EcPlatform platform = platformMap.get(shop.getPlatformId());
                if (platform != null) {
                    vo.setPlatformName(platform.getName());
                }
            }
        }
        vo.setPlatformOrderNo(order.getPlatformOrderNo());
        vo.setSource(order.getSource());
        vo.setStatus(order.getStatus());
        vo.setPlatformStatus(resolvePlatformStatus(order));
        vo.setExpressStationId(order.getExpressStationId());
        if (order.getExpressStationId() != null) {
            vo.setExpressStationName(stationNameMap.get(order.getExpressStationId()));
        }
        vo.setOrderTime(order.getOrderTime());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setBuyerName(order.getBuyerName());
        vo.setBuyerPhone(order.getBuyerPhone());
        vo.setReceiveProvince(order.getReceiveProvince());
        vo.setReceiveCity(order.getReceiveCity());
        vo.setReceiveDistrict(order.getReceiveDistrict());
        vo.setReceiveAddress(order.getReceiveAddress());
        vo.setTrackingNumber(order.getTrackingNumber());
        vo.setBuyerRemark(order.getBuyerRemark());
        vo.setSellerRemark(order.getSellerRemark());
        vo.setReceivedAmount(order.getReceivedAmount());
        vo.setTotalCostAmount(order.getTotalCostAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setEstimatedFreightAmount(order.getEstimatedFreightAmount());
        vo.setActualFreightAmount(order.getActualFreightAmount());
        vo.setOrderCouponAmount(order.getOrderCouponAmount());
        vo.setPlatformFeeAmount(order.getPlatformFeeAmount());
        vo.setProfitAmount(order.getProfitAmount());
        vo.setTotalLossAmount(order.getTotalLossAmount());
        vo.setHasShortage(order.getHasShortage() != null && order.getHasShortage() == 1);
        vo.setImportBatchId(order.getImportBatchId());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setLines(lines);
        vo.setLineCount(lineCount > 0 ? lineCount : lines.size());
        return vo;
    }

    private Map<Long, Integer> loadLineCountMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<EcSalesOrderLine> lines = ecSalesOrderLineMapper.selectList(new LambdaQueryWrapper<EcSalesOrderLine>()
                .in(EcSalesOrderLine::getOrderId, orderIds)
                .select(EcSalesOrderLine::getOrderId));
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (EcSalesOrderLine line : lines) {
            if (line.getOrderId() == null) {
                continue;
            }
            counts.merge(line.getOrderId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, EcSalesOrderLine> loadFirstLineMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<EcSalesOrderLine> lines = ecSalesOrderLineMapper.selectList(new LambdaQueryWrapper<EcSalesOrderLine>()
                .in(EcSalesOrderLine::getOrderId, orderIds)
                .orderByAsc(EcSalesOrderLine::getSortOrder)
                .orderByAsc(EcSalesOrderLine::getId));
        Map<Long, EcSalesOrderLine> result = new LinkedHashMap<>();
        for (EcSalesOrderLine line : lines) {
            if (line.getOrderId() != null) {
                result.putIfAbsent(line.getOrderId(), line);
            }
        }
        return result;
    }

    private EcSalesOrderLineVO toLineVO(EcSalesOrderLine line) {
        EcSalesOrderLineVO vo = new EcSalesOrderLineVO();
        vo.setId(line.getId());
        vo.setOrderId(line.getOrderId());
        vo.setSortOrder(line.getSortOrder());
        vo.setListingLinkSkuId(line.getListingLinkSkuId());
        vo.setLinkName(line.getLinkName());
        vo.setSkuSpecName(line.getSkuSpecName());
        vo.setSkuCodes(line.getSkuCodes());
        vo.setSkuQuantity(line.getSkuQuantity());
        vo.setShippedQuantity(line.getShippedQuantity());
        vo.setShortQuantity(line.getShortQuantity());
        vo.setStatus(line.getStatus());
        vo.setPlatformLineStatus(line.getPlatformLineStatus());
        vo.setRefundType(line.getRefundType());
        vo.setRefundTime(line.getRefundTime());
        vo.setRefundAmount(line.getRefundAmount());
        vo.setLossAmount(line.getLossAmount());
        vo.setUnitPrice(line.getUnitPrice());
        vo.setDiscountPct(line.getDiscountPct());
        vo.setLineCouponAmount(line.getLineCouponAmount());
        vo.setLineReceivedAmount(line.getLineReceivedAmount());
        vo.setSkuAmount(line.getSkuAmount());
        vo.setCartonAmount(line.getCartonAmount());
        vo.setExpressAmount(line.getExpressAmount());
        vo.setBaseCostAmount(line.getBaseCostAmount());
        vo.setPlatformFeeAmount(line.getPlatformFeeAmount());
        vo.setCostPrice(line.getCostPrice());
        vo.setMinSetAmount(line.getMinSetAmount());
        vo.setProfit(line.getProfit());
        vo.setPricingRisk(line.getPricingRisk());
        vo.setPlatformLineNo(line.getPlatformLineNo());
        vo.setPlatformItemName(line.getPlatformItemName());
        return vo;
    }

    private EcSalesOrderShortageVO toShortageVO(EcSalesOrderShortage s) {
        EcSalesOrderShortageVO vo = new EcSalesOrderShortageVO();
        vo.setId(s.getId());
        vo.setSkuCode(s.getSkuCode());
        vo.setNeedQty(s.getNeedQty());
        vo.setDeductedQty(s.getDeductedQty());
        vo.setShortQty(s.getShortQty());
        vo.setStatus(s.getStatus());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }

    private EcSalesOrderImportRowVO toImportRowVO(EcOrderImportRow row) {
        EcSalesOrderImportRowVO vo = new EcSalesOrderImportRowVO();
        vo.setId(row.getId());
        vo.setRowNo(row.getRowNo());
        vo.setParseStatus(row.getParseStatus());
        vo.setPlatformOrderNo(row.getPlatformOrderNo());
        vo.setLinkName(row.getLinkName());
        vo.setSkuSpecName(row.getSkuSpecName());
        vo.setSkuQuantity(parseSkuQuantityFromImportRow(row));
        vo.setMatchStatus(row.getMatchStatus());
        vo.setListingLinkSkuId(row.getListingLinkSkuId());
        vo.setManualCostPrice(row.getManualCostPrice());
        vo.setPlatformLineStatus(row.getPlatformLineStatus());
        vo.setLineStatus(row.getLineStatus());
        vo.setStatusMatchStatus(row.getStatusMatchStatus());
        vo.setSellerRemark(parseSellerRemarkFromImportRow(row));
        vo.setErrorMessage(row.getErrorMessage());
        return vo;
    }

    private Set<String> collect1688OrdersWithSellerRemark(List<Map<String, String>> rows) {
        Set<String> result = new LinkedHashSet<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, String> raw : rows) {
            String orderNo = getMapValue(raw, "platform_order_no", "platformOrderNo");
            if (!StringUtils.hasText(orderNo)) {
                continue;
            }
            if (StringUtils.hasText(getMapValue(raw, "seller_remark", "sellerRemark"))) {
                result.add(orderNo.trim());
            }
        }
        return result;
    }

    private boolean requiresManualCostForSellerRemark(EcOrderImportRow entity, Map<String, String> raw,
                                                      Set<String> sellerRemarkOrders) {
        if (!"OK".equals(entity.getParseStatus())) {
            return false;
        }
        if (StringUtils.hasText(entity.getPlatformOrderNo())
                && sellerRemarkOrders.contains(entity.getPlatformOrderNo().trim())) {
            return true;
        }
        return StringUtils.hasText(getMapValue(raw, "seller_remark", "sellerRemark"));
    }

    private String parseSellerRemarkFromImportRow(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            return trimToNull(getMapValueFromObject(map, "seller_remark", "sellerRemark"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseSkuQuantityFromImportRow(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            String qty = getMapValueFromObject(map, "sku_quantity", "skuQuantity", "quantity");
            if (!StringUtils.hasText(qty)) {
                return null;
            }
            String normalized = qty.trim();
            if (normalized.contains(".")) {
                normalized = normalized.substring(0, normalized.indexOf('.'));
            }
            int value = Integer.parseInt(normalized);
            return value > 0 ? value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 将订单实收均摊到同单各明细行，用于入库时计算行级利润。 */
    private List<BigDecimal> allocateOrderReceivedAmongLines(List<EcOrderImportRow> rows) {
        List<BigDecimal> result = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        BigDecimal orderTotal = null;
        for (EcOrderImportRow row : rows) {
            orderTotal = parseOrderReceivedFromImportRow(row);
            if (orderTotal != null) {
                break;
            }
        }
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            for (int i = 0; i < rows.size(); i++) {
                result.add(null);
            }
            return result;
        }
        int lineCount = rows.size();
        if (lineCount == 1) {
            result.add(orderTotal);
            return result;
        }
        BigDecimal perLine = orderTotal.divide(BigDecimal.valueOf(lineCount), 2, RoundingMode.HALF_UP);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < lineCount - 1; i++) {
            result.add(perLine);
            allocated = allocated.add(perLine);
        }
        result.add(orderTotal.subtract(allocated).setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private BigDecimal parseOrderReceivedFromImportRow(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            return parseOrderReceivedFromRaw(toStringMap(map));
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 订单实收：优先 received_amount，其次支付详情中的金额。 */
    private BigDecimal parseOrderReceivedFromRaw(Map<String, String> raw) {
        if (raw == null) {
            return null;
        }
        String value = getMapValue(raw, "received_amount", "receivedAmount");
        if (!StringUtils.hasText(value)) {
            value = SysImportParseSupport.extractAmountFromPayDetail(
                    getMapValue(raw, "pay_detail", "payDetail"));
        }
        return parseDecimal(value);
    }

    private Map<Long, EcShop> loadShopMap(List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Map.of();
        }
        return ecShopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(EcShop::getId, s -> s, (a, b) -> a));
    }

    private Map<Long, EcPlatform> loadPlatformMap(List<Long> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return Map.of();
        }
        return ecPlatformMapper.selectBatchIds(platformIds).stream()
                .collect(Collectors.toMap(EcPlatform::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, String> loadStationNameMap(List<Long> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return Map.of();
        }
        return ecExpressStationMapper.selectBatchIds(stationIds).stream()
                .collect(Collectors.toMap(EcExpressStation::getId, EcExpressStation::getName, (a, b) -> a));
    }

    private Long resolveExpressStationIdByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String trimmed = name.trim();
        EcExpressStation station = ecExpressStationMapper.selectOne(new LambdaQueryWrapper<EcExpressStation>()
                .eq(EcExpressStation::getName, trimmed)
                .last("LIMIT 1"));
        if (station != null) {
            return station.getId();
        }
        return expressStationNameAliasSupport.resolveStationId(trimmed);
    }

    private void apply1688LinkSkuParsing(Map<String, String> raw) {
        String combined = getMapValue(raw, "link_name", "linkName");
        if (!StringUtils.hasText(combined)) {
            return;
        }
        Ec1688ImportLinkNameSupport.ParsedLinkSku parsed = Ec1688ImportLinkNameSupport.parse(combined);
        if (StringUtils.hasText(parsed.linkName())) {
            raw.put("link_name", parsed.linkName());
        }
        if (StringUtils.hasText(parsed.skuSpecName())) {
            raw.put("sku_spec_name", parsed.skuSpecName());
        } else {
            raw.remove("sku_spec_name");
            raw.remove("skuSpecName");
        }
    }

    private boolean is1688Platform(Long platformId) {
        if (platformId == null) {
            return false;
        }
        EcPlatform platform = ecPlatformMapper.selectById(platformId);
        return platform != null
                && Objects.equals(platform.getPlatformCode(), EcPlatformCode.ALIBABA_1688.getCode());
    }

    private String getMapValue(Map<String, String> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key) && StringUtils.hasText(map.get(key))) {
                return map.get(key).trim();
            }
        }
        return null;
    }

    private String getMapValueFromObject(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && StringUtils.hasText(String.valueOf(val))) {
                return String.valueOf(val).trim();
            }
        }
        return null;
    }

    private BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = SysImportParseSupport.normalizeMoneyText(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimalOrZero(String value) {
        BigDecimal d = parseDecimal(value);
        return d != null ? d : BigDecimal.ZERO;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void shipOneLine(EcSalesOrder order, EcSalesOrderLine line) {
        if (!StringUtils.hasText(line.getSkuCodes())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "明细缺少货号，无法发货");
        }
        int sets = line.getSkuQuantity() != null ? line.getSkuQuantity() : 1;
        int lineShortSets = 0;
        boolean anyShort = false;
        for (String code : parseSkuCodes(line.getSkuCodes())) {
            EcSalesOrderInventorySupport.DeductOutcome outcome = inventorySupport.deductSkuCode(
                    order.getId(), line.getId(), code, sets);
            if (outcome.isHasShortage()) {
                anyShort = true;
                lineShortSets = Math.max(lineShortSets, (outcome.getShortQty() + sets - 1) / sets);
            }
        }
        line.setShippedQuantity(sets);
        line.setShortQuantity(anyShort ? Math.max(1, lineShortSets) : 0);
        line.setStatus(LINE_SHIPPED);
        ecSalesOrderLineMapper.updateById(line);
        if (anyShort) {
            order.setHasShortage(1);
            updateById(order);
        }
    }

    private List<String> parseSkuCodes(String skuCodes) {
        List<String> codes = new ArrayList<>();
        for (String part : skuCodes.split(",")) {
            if (StringUtils.hasText(part)) {
                codes.add(part.trim());
            }
        }
        return codes;
    }

    private EcSalesOrder createOrderFromImportRows(EcShop shop, SysImportBatch batch,
                                                   String platformOrderNo, List<EcOrderImportRow> rows,
                                                   EcImportStatusSupport statusSupport) {
        EcSalesOrder order = new EcSalesOrder();
        order.setOrderNo(generateOrderNo());
        order.setShopId(shop.getId());
        order.setPlatformOrderNo(platformOrderNo);
        order.setSource(SOURCE_IMPORT);
        order.setStatus(STATUS_PAID);
        order.setImportBatchId(batch.getId());
        order.setActualFreightAmount(BigDecimal.ZERO);
        order.setEstimatedFreightAmount(BigDecimal.ZERO);
        order.setHasShortage(0);
        applyImportHeaderFromRaw(order, rows);
        if (order.getOrderTime() == null) {
            order.setOrderTime(LocalDateTime.now());
        }
        save(order);

        insertImportLinesForOrder(order, rows, shop, statusSupport);
        recalculateOrderTotals(order.getId());
        syncOrderStatus(order.getId());
        return order;
    }

    private void replaceOrderFromImportRows(EcSalesOrder order, SysImportBatch batch,
                                            List<EcOrderImportRow> rows, EcShop shop,
                                            EcImportStatusSupport statusSupport) {
        if (inventorySupport.hasOrderInventoryDeduct(order.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "平台订单「" + order.getPlatformOrderNo() + "」已发货扣减库存，无法重复导入覆盖");
        }
        order.setImportBatchId(batch.getId());
        applyImportHeaderOverwriteFromRaw(order, rows);
        order.setHasShortage(0);
        updateById(order);
        ecSalesOrderLineMapper.delete(new LambdaQueryWrapper<EcSalesOrderLine>()
                .eq(EcSalesOrderLine::getOrderId, order.getId()));
        ecSalesOrderShortageMapper.delete(new LambdaQueryWrapper<EcSalesOrderShortage>()
                .eq(EcSalesOrderShortage::getOrderId, order.getId()));
        insertImportLinesForOrder(order, rows, shop, statusSupport);
        recalculateOrderTotals(order.getId());
        syncOrderStatus(order.getId());
    }

    private void insertImportLinesForOrder(EcSalesOrder order, List<EcOrderImportRow> rows, EcShop shop,
                                           EcImportStatusSupport statusSupport) {
        List<EcOrderImportRow> dedupedRows = dedupeImportRowsByLineKey(rows);
        List<BigDecimal> allocatedReceived = allocateOrderReceivedAmongLines(dedupedRows);
        int sort = 0;
        for (int i = 0; i < dedupedRows.size(); i++) {
            EcOrderImportRow row = dedupedRows.get(i);
            EcSalesOrderLineItem item = toImportLineItem(row, allocatedReceived.get(i));
            item.setSortOrder(sort++);
            BuiltLine built = buildSingleLine(item, shop, order.getExpressStationId(),
                    order.getReceiveProvince(), false);
            built.entity.setOrderId(order.getId());
            applyImportedLineStatus(built.entity,
                    resolveImportLineStatus(order, row, item, statusSupport));
            ecSalesOrderLineMapper.insert(built.entity);
            row.setSalesOrderId(order.getId());
            row.setSalesOrderLineId(built.entity.getId());
            ecOrderImportRowMapper.updateById(row);
        }
    }

    private List<EcOrderImportRow> dedupeImportRowsByLineKey(List<EcOrderImportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, EcOrderImportRow> deduped = new LinkedHashMap<>();
        for (EcOrderImportRow row : rows) {
            deduped.put(importLineKey(row), row);
        }
        return new ArrayList<>(deduped.values());
    }

    private String importLineKey(EcOrderImportRow row) {
        if (row.getListingLinkSkuId() != null) {
            return "sku:" + row.getListingLinkSkuId();
        }
        String link = row.getLinkName() != null ? row.getLinkName().trim() : "";
        String spec = row.getSkuSpecName() != null ? row.getSkuSpecName().trim() : "";
        if (StringUtils.hasText(link) || StringUtils.hasText(spec)) {
            return "link:" + link + "|" + spec;
        }
        return "row:" + row.getRowNo();
    }

    private void applyImportHeaderOverwriteFromRaw(EcSalesOrder order, List<EcOrderImportRow> rows) {
        if (rows == null) {
            return;
        }
        for (EcOrderImportRow row : rows) {
            mergeImportHeaderOverwriteFromRaw(order, row);
        }
    }

    private void mergeImportHeaderOverwriteFromRaw(EcSalesOrder order, EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            BigDecimal received = parseDecimal(getMapValueFromObject(map, "received_amount", "receivedAmount"));
            if (received != null) {
                order.setReceivedAmount(received);
            }
            String tracking = getMapValueFromObject(map, "tracking_number", "trackingNumber");
            if (StringUtils.hasText(tracking)) {
                order.setTrackingNumber(tracking);
            }
            String address = getMapValueFromObject(map, "receive_address", "receiveAddress");
            if (StringUtils.hasText(address)) {
                order.setReceiveAddress(address);
                order.setReceiveProvince(EcAddressProvinceSupport.parseProvince(order.getReceiveAddress()));
            }
            String buyerName = getMapValueFromObject(map, "buyer_name", "buyerName");
            if (StringUtils.hasText(buyerName)) {
                order.setBuyerName(buyerName);
            }
            String buyerPhone = getMapValueFromObject(map, "buyer_phone", "buyerPhone");
            if (StringUtils.hasText(buyerPhone)) {
                order.setBuyerPhone(buyerPhone);
            }
            LocalDateTime orderTime = parseImportDateTime(map, "order_time", "orderTime");
            if (orderTime != null) {
                order.setOrderTime(orderTime);
            }
            LocalDateTime payTime = parseImportDateTime(map, "pay_time", "payTime");
            if (payTime != null) {
                order.setPayTime(payTime);
            }
            LocalDateTime shipTime = parseImportDateTime(map, "ship_time", "shipTime");
            if (shipTime != null) {
                order.setShipTime(shipTime);
            }
            LocalDateTime completeTime = parseImportDateTime(map, "complete_time", "completeTime");
            if (completeTime != null) {
                order.setCompleteTime(completeTime);
            }
            String platformStatus = getMapValueFromObject(map, "platform_status", "platformStatus");
            if (!StringUtils.hasText(platformStatus)) {
                platformStatus = getMapValueFromObject(map, "platform_line_status", "platformLineStatus");
            }
            if (StringUtils.hasText(platformStatus)) {
                order.setPlatformStatus(trimToNull(platformStatus));
            }
            String stationName = getMapValueFromObject(map, "express_station_name", "expressStationName");
            if (StringUtils.hasText(stationName)) {
                order.setExpressStationId(resolveExpressStationIdByName(stationName));
            }
            String buyerRemark = getMapValueFromObject(map, "buyer_remark", "buyerRemark");
            if (StringUtils.hasText(buyerRemark)) {
                order.setBuyerRemark(buyerRemark);
            }
            String sellerRemark = getMapValueFromObject(map, "seller_remark", "sellerRemark");
            if (StringUtils.hasText(sellerRemark)) {
                order.setSellerRemark(sellerRemark);
            }
        } catch (Exception ignored) {
            /* optional fields */
        }
    }

    private String resolveImportLineStatus(EcSalesOrder order, EcOrderImportRow row, EcSalesOrderLineItem item,
                                           EcImportStatusSupport statusSupport) {
        String status;
        if (StringUtils.hasText(row.getLineStatus())) {
            status = row.getLineStatus();
        } else {
            String platformStatus = item.getPlatformLineStatus();
            if (!StringUtils.hasText(platformStatus)) {
                platformStatus = readImportPlatformStatusFromRow(row);
            }
            status = statusSupport.resolveLineStatus(platformStatus);
            if (!StringUtils.hasText(status)) {
                status = inferLineStatusFromRaw(readImportRowRawAsStringMap(row));
            }
        }
        return adjustImportLineStatusForCancelledOrder(order, status);
    }

    /**
     * 1688：订单头为已取消/交易关闭，子单显示已退款且有运单号 → 按退货退款(RETURNED)处理，而非已退款。
     */
    private String adjustImportLineStatusForCancelledOrder(EcSalesOrder order, String lineStatus) {
        if (!StringUtils.hasText(lineStatus)) {
            return lineStatus;
        }
        if (!"REFUNDED".equals(lineStatus.trim().toUpperCase())) {
            return lineStatus;
        }
        if (!isCancelledPlatformStatus(order.getPlatformStatus())) {
            return lineStatus;
        }
        if (!StringUtils.hasText(order.getTrackingNumber())) {
            return lineStatus;
        }
        return LINE_RETURNED;
    }

    private boolean isCancelledPlatformStatus(String platformStatus) {
        if (!StringUtils.hasText(platformStatus)) {
            return false;
        }
        String text = platformStatus.trim();
        if ("CANCELLED".equalsIgnoreCase(text)) {
            return true;
        }
        return text.contains("取消") || text.contains("交易关闭");
    }

    private void applyImportedLineStatus(EcSalesOrderLine line, String status) {
        String normalized = StringUtils.hasText(status) ? status.trim().toUpperCase() : LINE_PAID;
        int qty = line.getSkuQuantity() != null && line.getSkuQuantity() > 0 ? line.getSkuQuantity() : 1;
        switch (normalized) {
            case "COMPLETED" -> {
                line.setStatus(LINE_COMPLETED);
                line.setShippedQuantity(qty);
            }
            case "SHIPPED" -> {
                line.setStatus(LINE_SHIPPED);
                line.setShippedQuantity(qty);
            }
            case "CANCELLED" -> line.setStatus(LINE_CANCELLED);
            case "PARTIAL_REFUND" -> line.setStatus(LINE_PARTIAL_REFUND);
            case "REFUNDED" -> line.setStatus(LINE_REFUNDED);
            case "RETURNED" -> line.setStatus(LINE_RETURNED);
            default -> line.setStatus(LINE_PAID);
        }
    }

    private EcSalesOrderLineItem toImportLineItem(EcOrderImportRow row, BigDecimal lineReceivedAmount) {
        EcSalesOrderLineItem item = new EcSalesOrderLineItem();
        item.setListingLinkSkuId(row.getListingLinkSkuId());
        item.setLinkName(row.getLinkName());
        item.setSkuSpecName(row.getSkuSpecName());
        item.setSkuQuantity(parseImportQty(row));
        item.setManualCostPrice(row.getManualCostPrice());
        item.setLineReceivedAmount(lineReceivedAmount);
        item.setPlatformLineStatus(parseImportPlatformLineStatus(row));
        return item;
    }

    private String parseImportPlatformLineStatus(EcOrderImportRow row) {
        if (StringUtils.hasText(row.getPlatformLineStatus())) {
            return row.getPlatformLineStatus().trim();
        }
        return readImportPlatformStatus(readImportRowRawAsStringMap(row));
    }

    private void applyManualLinePricing(EcSalesOrderLine line, BigDecimal costPrice, BigDecimal lineReceived) {
        line.setCostPrice(costPrice);
        line.setBaseCostAmount(costPrice);
        int qty = line.getSkuQuantity() != null && line.getSkuQuantity() > 0 ? line.getSkuQuantity() : 1;
        if (lineReceived != null) {
            line.setLineReceivedAmount(lineReceived);
            BigDecimal totalCost = costPrice.multiply(new BigDecimal(qty)).setScale(2, RoundingMode.HALF_UP);
            line.setProfit(lineReceived.subtract(totalCost).setScale(2, RoundingMode.HALF_UP));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> toStringMap(Map<String, ?> raw) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : raw.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private String readRawText(Map<String, ?> raw, String... keys) {
        for (String key : keys) {
            Object val = raw.get(key);
            if (val == null) {
                continue;
            }
            if (val instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue()).stripTrailingZeros().toPlainString();
            }
            if (StringUtils.hasText(String.valueOf(val))) {
                return String.valueOf(val).trim();
            }
        }
        return null;
    }

    private int parseImportQty(EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return 1;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            String qty = getMapValueFromObject(map, "sku_quantity", "skuQuantity", "quantity");
            if (StringUtils.hasText(qty)) {
                return Math.max(1, Integer.parseInt(qty.trim()));
            }
        } catch (Exception ignored) {
            /* default 1 */
        }
        return 1;
    }

    private SysImportProfile resolveImportProfile(Long profileId, EcShop shop) {
        if (profileId != null) {
            SysImportProfile profile = sysImportProfileMapper.selectById(profileId);
            if (profile == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            return profile;
        }
        if (shop.getPlatformId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "店铺未关联平台，无法导入");
        }
        List<SysImportProfile> profiles = sysImportProfileMapper.selectList(new LambdaQueryWrapper<SysImportProfile>()
                .eq(SysImportProfile::getBizType, SysImportFieldRegistry.BIZ_SALES_ORDER)
                .eq(SysImportProfile::getPlatformId, shop.getPlatformId())
                .eq(SysImportProfile::getEnabled, 1)
                .orderByDesc(SysImportProfile::getUpdateTime));
        if (profiles.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未找到该平台导入配置，请先配置列对应关系");
        }
        EcPlatform platform = ecPlatformMapper.selectById(shop.getPlatformId());
        String preferredName = platform != null && StringUtils.hasText(platform.getName())
                ? platform.getName().trim() + "excel模版" : null;
        if (preferredName != null) {
            for (SysImportProfile profile : profiles) {
                if (preferredName.equals(profile.getName())) {
                    return profile;
                }
            }
        }
        return profiles.get(0);
    }

    private void applyImportHeaderFromRaw(EcSalesOrder order, List<EcOrderImportRow> rows) {
        if (rows == null) {
            return;
        }
        for (EcOrderImportRow row : rows) {
            mergeImportHeaderFromRaw(order, row);
        }
    }

    private void mergeImportHeaderFromRaw(EcSalesOrder order, EcOrderImportRow row) {
        if (!StringUtils.hasText(row.getRawJson())) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
            if (order.getReceivedAmount() == null) {
                order.setReceivedAmount(parseDecimal(getMapValueFromObject(map, "received_amount", "receivedAmount")));
            }
            if (!StringUtils.hasText(order.getTrackingNumber())) {
                order.setTrackingNumber(getMapValueFromObject(map, "tracking_number", "trackingNumber"));
            }
            if (!StringUtils.hasText(order.getReceiveAddress())) {
                order.setReceiveAddress(getMapValueFromObject(map, "receive_address", "receiveAddress"));
                order.setReceiveProvince(EcAddressProvinceSupport.parseProvince(order.getReceiveAddress()));
            }
            if (!StringUtils.hasText(order.getBuyerName())) {
                order.setBuyerName(getMapValueFromObject(map, "buyer_name", "buyerName"));
            }
            if (!StringUtils.hasText(order.getBuyerPhone())) {
                order.setBuyerPhone(getMapValueFromObject(map, "buyer_phone", "buyerPhone"));
            }
            if (order.getOrderTime() == null) {
                LocalDateTime orderTime = parseImportDateTime(map, "order_time", "orderTime");
                if (orderTime != null) {
                    order.setOrderTime(orderTime);
                }
            }
            if (order.getPayTime() == null) {
                LocalDateTime payTime = parseImportDateTime(map, "pay_time", "payTime");
                if (payTime != null) {
                    order.setPayTime(payTime);
                }
            }
            if (order.getShipTime() == null) {
                LocalDateTime shipTime = parseImportDateTime(map, "ship_time", "shipTime");
                if (shipTime != null) {
                    order.setShipTime(shipTime);
                }
            }
            if (order.getCompleteTime() == null) {
                LocalDateTime completeTime = parseImportDateTime(map, "complete_time", "completeTime");
                if (completeTime != null) {
                    order.setCompleteTime(completeTime);
                }
            }
            if (!StringUtils.hasText(order.getPlatformStatus())) {
                String platformStatus = getMapValueFromObject(map, "platform_status", "platformStatus");
                if (!StringUtils.hasText(platformStatus)) {
                    platformStatus = getMapValueFromObject(map, "platform_line_status", "platformLineStatus");
                }
                order.setPlatformStatus(trimToNull(platformStatus));
            }
            if (order.getExpressStationId() == null) {
                String stationName = getMapValueFromObject(map, "express_station_name", "expressStationName");
                if (StringUtils.hasText(stationName)) {
                    order.setExpressStationId(resolveExpressStationIdByName(stationName));
                }
            }
            if (!StringUtils.hasText(order.getBuyerRemark())) {
                order.setBuyerRemark(trimToNull(getMapValueFromObject(map, "buyer_remark", "buyerRemark")));
            }
            if (!StringUtils.hasText(order.getSellerRemark())) {
                order.setSellerRemark(trimToNull(getMapValueFromObject(map, "seller_remark", "sellerRemark")));
            }
        } catch (Exception ignored) {
            /* optional fields */
        }
    }

    private LocalDateTime parseImportDateTime(Map<String, Object> map, String... keys) {
        String value = getMapValueFromObject(map, keys);
        return SysImportParseSupport.tryParseDateTime(value);
    }

    private EcSalesOrder requireDeletableOrder(Long id) {
        EcSalesOrder order = requireOrder(id);
        if (STATUS_DRAFT.equals(order.getStatus())) {
            return order;
        }
        if (SOURCE_MANUAL.equals(order.getSource())) {
            return order;
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前订单不可删除");
    }

    private EcSalesOrder requireEditableOrder(Long id) {
        EcSalesOrder order = requireOrder(id);
        if (STATUS_DRAFT.equals(order.getStatus())) {
            return order;
        }
        if (SOURCE_MANUAL.equals(order.getSource())) {
            return order;
        }
        if (SOURCE_IMPORT.equals(order.getSource())
                && (STATUS_PAID.equals(order.getStatus()) || STATUS_CANCELLED.equals(order.getStatus()))) {
            return order;
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前订单状态不可编辑");
    }

    private EcSalesOrder requireOrder(Long id) {
        EcSalesOrder order = getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    private EcSalesOrder requireDraftOrder(Long id) {
        EcSalesOrder order = requireOrder(id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅草稿订单可编辑或删除");
        }
        return order;
    }

    private EcSalesOrderLine requireLine(Long orderId, Long lineId) {
        EcSalesOrderLine line = ecSalesOrderLineMapper.selectById(lineId);
        if (line == null || !Objects.equals(line.getOrderId(), orderId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return line;
    }

    private EcShop requireShop(Long shopId) {
        if (shopId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择店铺");
        }
        EcShop shop = ecShopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "店铺不存在");
        }
        return shop;
    }

    private String generateOrderNo() {
        String prefix = "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = count(new LambdaQueryWrapper<EcSalesOrder>().likeRight(EcSalesOrder::getOrderNo, prefix)) + 1;
        return prefix + String.format("%04d", count);
    }

    private String generateBatchNo() {
        String prefix = "IMP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = sysImportBatchMapper.selectCount(new LambdaQueryWrapper<SysImportBatch>()
                .likeRight(SysImportBatch::getBatchNo, prefix)) + 1;
        return prefix + String.format("%04d", count);
    }

    private Long readShopIdFromBatch(SysImportBatch batch) {
        if (!StringUtils.hasText(batch.getBizContext())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入批次缺少店铺信息");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> ctx = objectMapper.readValue(batch.getBizContext(), Map.class);
            Object shopId = ctx.get("shopId");
            if (shopId == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入批次缺少店铺信息");
            }
            return Long.valueOf(String.valueOf(shopId));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入批次上下文无效");
        }
    }

    private String buildSalesOrderBatchContext(Long shopId, String orderMonth,
                                               EcSalesOrderImportFileStorage.SaveResult fileMeta) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("shopId", shopId);
        if (StringUtils.hasText(orderMonth)) {
            ctx.put("orderMonth", orderMonth.trim());
        }
        if (fileMeta != null) {
            ctx.put("localStoredName", fileMeta.localStoredName());
            ctx.put("fileSize", fileMeta.fileSize());
            if (fileMeta.storageFsId() != null) {
                ctx.put("storageFsId", fileMeta.storageFsId());
            }
        }
        return toJson(ctx);
    }

    private EcSalesOrderImportPreviewVO enrichImportPreviewVO(EcSalesOrderImportPreviewVO vo,
                                                              SysImportBatch batch,
                                                              Long shopId) {
        vo.setShopId(shopId);
        vo.setProfileId(batch.getProfileId());
        vo.setFileName(batch.getFileName());
        vo.setFileSize(readFileSizeFromBatch(batch));
        List<String> columns = parseDetectedColumnNames(batch.getDetectedColumns());
        vo.setDetectedColumns(columns);
        vo.setDetectedColumnCount(columns.isEmpty() ? null : columns.size());
        vo.setImportFileReadable(importFileStorage.exists(batch));
        return vo;
    }

    private Long readFileSizeFromBatch(SysImportBatch batch) {
        if (!StringUtils.hasText(batch.getBizContext())) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> ctx = objectMapper.readValue(batch.getBizContext(), Map.class);
            Object fileSize = ctx.get("fileSize");
            if (fileSize == null) {
                return null;
            }
            return Long.valueOf(String.valueOf(fileSize));
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> parseDetectedColumnNames(String detectedColumnsJson) {
        if (!StringUtils.hasText(detectedColumnsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(detectedColumnsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Optional<YearMonth> readOrderMonthFromBatch(SysImportBatch batch) {
        if (!StringUtils.hasText(batch.getBizContext())) {
            return Optional.empty();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> ctx = objectMapper.readValue(batch.getBizContext(), Map.class);
            Object orderMonth = ctx.get("orderMonth");
            if (orderMonth == null || !StringUtils.hasText(String.valueOf(orderMonth))) {
                return Optional.empty();
            }
            return Optional.of(YearMonth.parse(String.valueOf(orderMonth).trim()));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<YearMonth> resolveBatchOrderMonth(SysImportBatch batch) {
        Optional<YearMonth> fromContext = readOrderMonthFromBatch(batch);
        if (fromContext.isPresent()) {
            return fromContext;
        }
        List<EcOrderImportRow> rows = ecOrderImportRowMapper.selectList(new LambdaQueryWrapper<EcOrderImportRow>()
                .eq(EcOrderImportRow::getBatchId, batch.getId())
                .orderByAsc(EcOrderImportRow::getRowNo)
                .last("LIMIT 30"));
        for (EcOrderImportRow row : rows) {
            if (!StringUtils.hasText(row.getRawJson())) {
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(row.getRawJson(), Map.class);
                LocalDateTime orderTime = parseImportDateTime(map, "order_time", "orderTime");
                if (orderTime != null) {
                    return Optional.of(YearMonth.from(orderTime));
                }
            } catch (Exception ignored) {
                // try next row
            }
        }
        return Optional.empty();
    }

    private void applyOrderMonthFilter(LambdaQueryWrapper<EcSalesOrder> wrapper, String orderMonth) {
        if (!StringUtils.hasText(orderMonth)) {
            return;
        }
        YearMonth ym = parseOrderMonth(orderMonth);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        wrapper.isNotNull(EcSalesOrder::getOrderTime)
                .ge(EcSalesOrder::getOrderTime, start)
                .lt(EcSalesOrder::getOrderTime, end);
    }

    private void applyOrderTimeRangeFilter(LambdaQueryWrapper<EcSalesOrder> wrapper,
                                           String orderTimeFrom, String orderTimeTo) {
        wrapper.isNotNull(EcSalesOrder::getOrderTime);
        if (StringUtils.hasText(orderTimeFrom)) {
            wrapper.ge(EcSalesOrder::getOrderTime, parseOrderTimeFrom(orderTimeFrom));
        }
        if (StringUtils.hasText(orderTimeTo)) {
            wrapper.lt(EcSalesOrder::getOrderTime, parseOrderTimeToExclusive(orderTimeTo));
        }
    }

    private LocalDateTime parseOrderTimeFrom(String value) {
        String trimmed = value.trim();
        if (trimmed.length() <= 10) {
            return LocalDate.parse(trimmed.substring(0, 10), ORDER_DATE_FORMAT).atStartOfDay();
        }
        return LocalDateTime.parse(trimmed.replace('T', ' ').substring(0, 19), ORDER_TIME_FORMAT);
    }

    private LocalDateTime parseOrderTimeToExclusive(String value) {
        String trimmed = value.trim();
        if (trimmed.length() <= 10) {
            return LocalDate.parse(trimmed.substring(0, 10), ORDER_DATE_FORMAT).plusDays(1).atStartOfDay();
        }
        LocalDateTime dt = LocalDateTime.parse(trimmed.replace('T', ' ').substring(0, 19), ORDER_TIME_FORMAT);
        if (trimmed.length() <= 16) {
            return dt.plusDays(1);
        }
        return dt.plusSeconds(1);
    }

    private YearMonth parseOrderMonth(String orderMonth) {
        if (!StringUtils.hasText(orderMonth)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择订单月份");
        }
        try {
            return YearMonth.parse(orderMonth.trim(), DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单月份格式无效");
        }
    }

    private List<EcShop> loadOverviewShops(Long shopId) {
        LambdaQueryWrapper<EcShop> wrapper = new LambdaQueryWrapper<EcShop>()
                .orderByAsc(EcShop::getName);
        if (shopId != null) {
            wrapper.eq(EcShop::getId, shopId);
        }
        return ecShopMapper.selectList(wrapper);
    }

    private Map<Long, Long> countOrdersByShop(LocalDateTime start, LocalDateTime end, Long shopId) {
        LambdaQueryWrapper<EcSalesOrder> wrapper = new LambdaQueryWrapper<EcSalesOrder>()
                .ge(EcSalesOrder::getOrderTime, start)
                .lt(EcSalesOrder::getOrderTime, end)
                .isNotNull(EcSalesOrder::getOrderTime);
        if (shopId != null) {
            wrapper.eq(EcSalesOrder::getShopId, shopId);
        }
        List<EcSalesOrder> orders = list(wrapper);
        return orders.stream()
                .collect(Collectors.groupingBy(EcSalesOrder::getShopId, Collectors.counting()));
    }

    private Map<Long, String> loadPlatformNameMap(List<Long> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return Map.of();
        }
        return ecPlatformMapper.selectBatchIds(platformIds).stream()
                .collect(Collectors.toMap(EcPlatform::getId, EcPlatform::getName, (a, b) -> a));
    }

    private Map<Long, PendingBatchInfo> loadPendingBatchInfoByShop(YearMonth selectedMonth) {
        List<SysImportBatch> batches = sysImportBatchMapper.selectList(new LambdaQueryWrapper<SysImportBatch>()
                .eq(SysImportBatch::getBizType, SysImportFieldRegistry.BIZ_SALES_ORDER)
                .eq(SysImportBatch::getStatus, "PREVIEWED")
                .orderByDesc(SysImportBatch::getUpdateTime));
        Map<Long, PendingBatchInfo> result = new LinkedHashMap<>();
        for (SysImportBatch batch : batches) {
            Optional<YearMonth> batchMonth = resolveBatchOrderMonth(batch);
            if (batchMonth.isEmpty() || !batchMonth.get().equals(selectedMonth)) {
                continue;
            }
            Long shopId;
            try {
                shopId = readShopIdFromBatch(batch);
            } catch (BusinessException ex) {
                continue;
            }
            if (result.containsKey(shopId)) {
                continue;
            }
            int reviewRows = countPendingReviewRows(batch.getId());
            LocalDateTime importTime = batch.getUpdateTime() != null ? batch.getUpdateTime() : batch.getCreateTime();
            result.put(shopId, new PendingBatchInfo(batch.getId(), reviewRows, importTime));
        }
        return result;
    }

    private int countPendingReviewRows(Long batchId) {
        List<EcOrderImportRow> rows = ecOrderImportRowMapper.selectList(new LambdaQueryWrapper<EcOrderImportRow>()
                .eq(EcOrderImportRow::getBatchId, batchId));
        int count = 0;
        for (EcOrderImportRow row : rows) {
            if (!"OK".equals(row.getParseStatus())) {
                continue;
            }
            if ("UNMATCHED".equals(row.getStatusMatchStatus())
                    && !StringUtils.hasText(row.getLineStatus())) {
                count++;
                continue;
            }
            if ("UNMATCHED".equals(row.getMatchStatus())) {
                if (row.getManualCostPrice() == null
                        || row.getManualCostPrice().compareTo(BigDecimal.ZERO) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 同一店铺同月仅保留一个有效待核对批次，避免确认入库后仍被旧 PREVIEWED 批次影响卡片状态。
     */
    private void closeOtherPreviewedBatches(Long shopId, YearMonth month, Long keepBatchId) {
        List<SysImportBatch> batches = sysImportBatchMapper.selectList(new LambdaQueryWrapper<SysImportBatch>()
                .eq(SysImportBatch::getBizType, SysImportFieldRegistry.BIZ_SALES_ORDER)
                .eq(SysImportBatch::getStatus, "PREVIEWED")
                .ne(keepBatchId != null, SysImportBatch::getId, keepBatchId));
        for (SysImportBatch batch : batches) {
            Long batchShopId;
            try {
                batchShopId = readShopIdFromBatch(batch);
            } catch (BusinessException ex) {
                continue;
            }
            if (!shopId.equals(batchShopId)) {
                continue;
            }
            Optional<YearMonth> batchMonth = resolveBatchOrderMonth(batch);
            if (batchMonth.isEmpty() || !batchMonth.get().equals(month)) {
                continue;
            }
            batch.setStatus("FAILED");
            sysImportBatchMapper.updateById(batch);
        }
    }

    private Optional<YearMonth> parseOrderMonthOptional(String orderMonth) {
        if (!StringUtils.hasText(orderMonth)) {
            return Optional.empty();
        }
        try {
            return Optional.of(YearMonth.parse(orderMonth.trim(), DateTimeFormatter.ofPattern("yyyy-MM")));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private LocalDateTime loadShopLastImportTime(Long shopId, LocalDateTime start, LocalDateTime end) {
        List<SysImportBatch> batches = sysImportBatchMapper.selectList(new LambdaQueryWrapper<SysImportBatch>()
                .eq(SysImportBatch::getBizType, SysImportFieldRegistry.BIZ_SALES_ORDER)
                .eq(SysImportBatch::getStatus, "COMMITTED")
                .isNotNull(SysImportBatch::getCommittedTime)
                .ge(SysImportBatch::getCommittedTime, start)
                .lt(SysImportBatch::getCommittedTime, end)
                .orderByDesc(SysImportBatch::getCommittedTime));
        for (SysImportBatch batch : batches) {
            try {
                if (shopId.equals(readShopIdFromBatch(batch))) {
                    return batch.getCommittedTime();
                }
            } catch (BusinessException ex) {
                continue;
            }
        }
        return null;
    }

    private EcImportStatusSupport createStatusSupport(SysImportProfile profile) {
        return EcImportStatusSupport.from(profile, objectMapper,
                ecSystemSettingsService.resolveOrderImportStatusMapping(),
                ecSystemSettingsService.resolveOrderImportDefaultLineStatus());
    }

    private static class BuiltLine {
        EcSalesOrderLine entity;
    }
}
