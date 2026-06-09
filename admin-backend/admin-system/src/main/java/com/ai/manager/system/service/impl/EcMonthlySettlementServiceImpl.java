package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcSettlementExpressBillManualSaveRequest;
import com.ai.manager.system.domain.dto.EcSettlementBuyerExcludeSaveRequest;
import com.ai.manager.system.domain.dto.EcSettlementOrderDecisionBatchRequest;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.entity.EcSalesOrder;
import com.ai.manager.system.domain.entity.EcSalesOrderLine;
import com.ai.manager.system.domain.entity.EcSettlementBuyerExclude;
import com.ai.manager.system.domain.entity.EcSettlementExpressBill;
import com.ai.manager.system.domain.entity.EcSettlementExpressBillLine;
import com.ai.manager.system.domain.entity.EcSettlementOrderDecision;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO;
import com.ai.manager.system.domain.vo.EcSettlementBuyerExcludeVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillImportVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillLineVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillPreviewVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillRecordVO;
import com.ai.manager.system.domain.vo.SysImportProfileVO;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.ai.manager.system.mapper.EcSalesOrderLineMapper;
import com.ai.manager.system.mapper.EcSalesOrderMapper;
import com.ai.manager.system.mapper.EcSettlementBuyerExcludeMapper;
import com.ai.manager.system.mapper.EcSettlementExpressBillLineMapper;
import com.ai.manager.system.mapper.EcSettlementExpressBillMapper;
import com.ai.manager.system.mapper.EcSettlementOrderDecisionMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.service.EcMonthlySettlementService;
import com.ai.manager.system.service.SysImportService;
import com.ai.manager.system.service.support.EcExpressBillParseSupport;
import com.ai.manager.system.service.support.EcExpressBillParseSupport.ExpressBillRow;
import com.ai.manager.system.service.support.ExpressBillStationFilter;
import com.ai.manager.system.service.support.SysImportColumnMappingSupport;
import com.ai.manager.system.service.support.SysImportFieldRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EcMonthlySettlementServiceImpl implements EcMonthlySettlementService {

    private static final Set<String> AUTO_EXCLUDE_STATUS = Set.of("REFUNDED", "CANCELLED");
    private static final Set<String> PENDING_STATUS = Set.of("DRAFT", "PAID", "PARTIAL_SHIPPED", "PARTIAL_REFUND");
    private static final Set<String> AUTO_INCLUDE_STATUS = Set.of("SHIPPED", "COMPLETED");
    private static final String LINE_RETURNED = "RETURNED";

    private final EcSalesOrderMapper ecSalesOrderMapper;
    private final EcSalesOrderLineMapper ecSalesOrderLineMapper;
    private final EcShopMapper ecShopMapper;
    private final EcSettlementBuyerExcludeMapper buyerExcludeMapper;
    private final EcSettlementOrderDecisionMapper orderDecisionMapper;
    private final EcSettlementExpressBillMapper expressBillMapper;
    private final EcSettlementExpressBillLineMapper expressBillLineMapper;
    private final EcExpressStationMapper expressStationMapper;
    private final SysImportService sysImportService;
    private final SysImportColumnMappingSupport columnMappingSupport;

    @Override
    public EcMonthlySettlementVO calculate(String settlementMonth, Long shopId) {
        YearMonth ym = parseMonth(settlementMonth);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<EcShop> shops = loadShops(shopId);
        Map<Long, String> shopNameMap = shops.stream()
                .collect(Collectors.toMap(EcShop::getId, EcShop::getName, (a, b) -> a));

        EcMonthlySettlementVO vo = new EcMonthlySettlementVO();
        vo.setSettlementMonth(ym.toString());
        List<EcMonthlySettlementVO.ShopSummary> summaries = new ArrayList<>();

        for (EcShop shop : shops) {
            summaries.add(buildShopSummary(shop, shopNameMap, start, end, ym.toString()));
        }
        vo.setShops(summaries);
        vo.setExpressBillImported(isExpressBillImported(ym.toString()));
        return vo;
    }

    @Override
    public boolean isExpressBillImported(String billMonth) {
        String month = parseMonth(billMonth).toString();
        Long count = expressBillMapper.selectCount(new LambdaQueryWrapper<EcSettlementExpressBill>()
                .eq(EcSettlementExpressBill::getBillMonth, month)
                .eq(EcSettlementExpressBill::getStatus, "IMPORTED"));
        return count != null && count > 0;
    }

    @Override
    public List<EcSettlementBuyerExcludeVO> listBuyerExcludes(Long shopId) {
        LambdaQueryWrapper<EcSettlementBuyerExclude> wrapper = new LambdaQueryWrapper<EcSettlementBuyerExclude>()
                .eq(EcSettlementBuyerExclude::getEnabled, 1)
                .orderByDesc(EcSettlementBuyerExclude::getUpdateTime);
        if (shopId != null) {
            wrapper.and(w -> w.eq(EcSettlementBuyerExclude::getShopId, shopId)
                    .or().isNull(EcSettlementBuyerExclude::getShopId));
        }
        List<EcSettlementBuyerExclude> list = buyerExcludeMapper.selectList(wrapper);
        Map<Long, String> shopNames = loadShopNameMap(list.stream()
                .map(EcSettlementBuyerExclude::getShopId).filter(Objects::nonNull).distinct().toList());
        List<EcSettlementBuyerExcludeVO> result = new ArrayList<>();
        for (EcSettlementBuyerExclude item : list) {
            EcSettlementBuyerExcludeVO row = new EcSettlementBuyerExcludeVO();
            row.setId(item.getId());
            row.setShopId(item.getShopId());
            row.setShopName(item.getShopId() != null ? shopNames.get(item.getShopId()) : null);
            row.setBuyerName(item.getBuyerName());
            row.setRemark(item.getRemark());
            row.setEnabled(item.getEnabled());
            row.setCreateTime(item.getCreateTime());
            result.add(row);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSettlementBuyerExcludeVO saveBuyerExclude(EcSettlementBuyerExcludeSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getBuyerName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "买家昵称不能为空");
        }
        EcSettlementBuyerExclude entity;
        if (request.getId() != null) {
            entity = buyerExcludeMapper.selectById(request.getId());
            if (entity == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
        } else {
            entity = new EcSettlementBuyerExclude();
            entity.setEnabled(1);
        }
        entity.setShopId(request.getShopId());
        entity.setBuyerName(request.getBuyerName().trim());
        entity.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (entity.getId() == null) {
            buyerExcludeMapper.insert(entity);
        } else {
            buyerExcludeMapper.updateById(entity);
        }
        EcSettlementBuyerExcludeVO vo = new EcSettlementBuyerExcludeVO();
        vo.setId(entity.getId());
        vo.setShopId(entity.getShopId());
        vo.setBuyerName(entity.getBuyerName());
        vo.setRemark(entity.getRemark());
        vo.setEnabled(entity.getEnabled());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBuyerExclude(Long id) {
        if (buyerExcludeMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        buyerExcludeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcMonthlySettlementVO saveOrderDecisions(EcSettlementOrderDecisionBatchRequest request) {
        if (request == null || !StringUtils.hasText(request.getSettlementMonth())
                || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请提供统计月份与订单决策");
        }
        String month = parseMonth(request.getSettlementMonth()).toString();
        for (EcSettlementOrderDecisionBatchRequest.Item item : request.getItems()) {
            if (item.getOrderId() == null || item.getIncluded() == null) {
                continue;
            }
            EcSalesOrder order = ecSalesOrderMapper.selectById(item.getOrderId());
            if (order == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单不存在: " + item.getOrderId());
            }
            EcSettlementOrderDecision existing = orderDecisionMapper.selectOne(
                    new LambdaQueryWrapper<EcSettlementOrderDecision>()
                            .eq(EcSettlementOrderDecision::getOrderId, item.getOrderId())
                            .eq(EcSettlementOrderDecision::getSettlementMonth, month));
            if (existing == null) {
                existing = new EcSettlementOrderDecision();
                existing.setShopId(order.getShopId());
                existing.setOrderId(order.getId());
                existing.setSettlementMonth(month);
                existing.setIncluded(Boolean.TRUE.equals(item.getIncluded()) ? 1 : 0);
                orderDecisionMapper.insert(existing);
            } else {
                existing.setIncluded(Boolean.TRUE.equals(item.getIncluded()) ? 1 : 0);
                orderDecisionMapper.updateById(existing);
            }
        }
        return calculate(month, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSettlementExpressBillImportVO importExpressBill(String billMonth, Long expressStationId,
                                                             MultipartFile file,
                                                             Map<String, String> columnMapping,
                                                             Integer headerRow, Integer dataStartRow,
                                                             Boolean includeLabelPrice) {
        if (expressStationId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择快递公司");
        }
        ExpressBillStationFilter stationFilter = ExpressBillStationFilter.parse(expressStationId);
        if (stationFilter.otherExpress()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件导入请选择具体快递公司");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请上传快递账单文件");
        }
        parseMonth(billMonth);
        EcExpressStation station = requireExpressStation(stationFilter.stationId());
        SysImportProfileVO savedProfile = sysImportService.getProfileByScope(
                SysImportFieldRegistry.BIZ_SETTLEMENT_EXPRESS_BILL,
                SysImportColumnMappingSupport.expressStationScopeKey(stationFilter.stationId()));
        int header = headerRow == null || headerRow < 1
                ? (savedProfile.getHeaderRow() == null ? 1 : savedProfile.getHeaderRow())
                : headerRow;
        int dataStart = dataStartRow == null || dataStartRow < 1
                ? (savedProfile.getDataStartRow() == null ? header + 1 : savedProfile.getDataStartRow())
                : dataStartRow;
        Map<String, String> mapping = resolveColumnMapping(stationFilter.stationId(), columnMapping, file, header);
        List<String> fileHeaders = EcExpressBillParseSupport.readHeaderColumns(file, header);
        mapping = EcExpressBillParseSupport.enrichColumnMapping(fileHeaders, mapping);
        List<ExpressBillRow> rows = EcExpressBillParseSupport.parseRows(file, mapping, header, dataStart);
        if (rows.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账单无有效数据");
        }

        EcSettlementExpressBill bill = new EcSettlementExpressBill();
        bill.setBillMonth(billMonth);
        bill.setExpressStationId(stationFilter.stationId());
        bill.setOtherExpress(0);
        bill.setIncludeLabelPrice(Boolean.TRUE.equals(includeLabelPrice) ? 1 : 0);
        bill.setColumnMapping(columnMappingSupport.writeColumnMapping(
                SysImportFieldRegistry.BIZ_SETTLEMENT_EXPRESS_BILL, mapping));
        bill.setHeaderRow(header);
        bill.setDataStartRow(dataStart);
        bill.setImportMode("FILE");
        bill.setFileName(file.getOriginalFilename());
        bill.setStatus("IMPORTED");
        expressBillMapper.insert(bill);

        int overwritten = 0;
        boolean addLabelPrice = Boolean.TRUE.equals(includeLabelPrice);
        Map<String, EcSettlementExpressBillLine> lineByTracking =
                loadExistingLinesByTracking(billMonth, stationFilter);
        Set<String> processedThisRun = new HashSet<>();
        for (ExpressBillRow row : rows) {
            String tracking = row.trackingNumber();
            processedThisRun.add(tracking);
            EcSettlementExpressBillLine line = lineByTracking.get(tracking);
            boolean updating = line != null && line.getId() != null;
            if (line == null) {
                line = new EcSettlementExpressBillLine();
                line.setSource("FILE");
            }
            line.setBillId(bill.getId());
            line.setExpressStationId(stationFilter.stationId());
            line.setTrackingNumber(tracking);
            BigDecimal appliedFreight = resolveAppliedFreight(row.freightAmount(), station, addLabelPrice);
            line.setFreightAmount(appliedFreight);
            line.setSettlementDestination(row.settlementDestination());
            line.setWeight(row.weight());
            line.setShipTime(row.shipTime());
            line.setOrderId(null);
            line.setOrderNo(null);
            line.setPlatformOrderNo(null);
            List<EcSalesOrder> orders = findOrdersForBillRow(row);
            if (orders.isEmpty()) {
                line.setMatchStatus("UNMATCHED");
            } else {
                line.setMatchStatus("MATCHED");
                EcSalesOrder primary = orders.get(0);
                line.setOrderId(primary.getId());
                line.setOrderNo(primary.getOrderNo());
                line.setPlatformOrderNo(primary.getPlatformOrderNo());
                if (line.getShipTime() == null) {
                    line.setShipTime(primary.getShipTime());
                }
                if (!StringUtils.hasText(line.getSettlementDestination())) {
                    line.setSettlementDestination(resolveOrderDestination(primary));
                }
                applyFreightToOrders(orders, appliedFreight, station, false);
            }
            if (line.getId() == null) {
                expressBillLineMapper.insert(line);
            } else {
                expressBillLineMapper.updateById(line);
                if (updating) {
                    overwritten++;
                }
            }
            lineByTracking.put(tracking, line);
        }

        int matched = 0;
        int unmatched = 0;
        for (String tracking : processedThisRun) {
            EcSettlementExpressBillLine line = lineByTracking.get(tracking);
            if (line == null) {
                continue;
            }
            if ("MATCHED".equals(line.getMatchStatus())) {
                matched++;
            } else if ("UNMATCHED".equals(line.getMatchStatus())) {
                unmatched++;
            }
        }

        bill.setTotalRows(processedThisRun.size());
        bill.setMatchedRows(matched);
        bill.setUnmatchedRows(unmatched);
        bill.setGapOrderRows(0);
        expressBillMapper.updateById(bill);

        EcSettlementExpressBillImportVO vo = toImportVO(bill, station.getName(),
                countManualPendingOrders(bill.getBillMonth(), stationFilter));
        vo.setOverwrittenRows(overwritten);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSettlementExpressBillImportVO prepareManualExpressBill(String billMonth, Long expressStationId,
                                                                    Boolean includeLabelPrice) {
        ExpressBillStationFilter stationFilter = ExpressBillStationFilter.parse(expressStationId);
        parseMonth(billMonth);
        EcExpressStation station = stationFilter.otherExpress()
                ? null : requireExpressStation(stationFilter.stationId());

        EcSettlementExpressBill bill = new EcSettlementExpressBill();
        bill.setBillMonth(billMonth);
        bill.setExpressStationId(stationFilter.billStationId());
        bill.setOtherExpress(stationFilter.billOtherExpressFlag());
        bill.setIncludeLabelPrice(Boolean.TRUE.equals(includeLabelPrice) && !stationFilter.otherExpress() ? 1 : 0);
        bill.setImportMode("MANUAL");
        bill.setHeaderRow(1);
        bill.setDataStartRow(2);
        bill.setTotalRows(0);
        bill.setMatchedRows(0);
        bill.setUnmatchedRows(0);
        bill.setStatus("IMPORTED");
        expressBillMapper.insert(bill);

        int gapCount = appendGapOrderLines(bill.getId(), billMonth, stationFilter, Set.of());
        bill.setGapOrderRows(gapCount);
        expressBillMapper.updateById(bill);

        String stationName = stationFilter.otherExpress() ? "其他快递公司" : station.getName();
        return toImportVO(bill, stationName,
                countManualPendingOrders(bill.getBillMonth(), stationFilter));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcSettlementExpressBillImportVO saveManualExpressBillLines(EcSettlementExpressBillManualSaveRequest request) {
        if (request == null || request.getBillId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请指定账单批次");
        }
        EcSettlementExpressBill bill = expressBillMapper.selectById(request.getBillId());
        if (bill == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账单批次不存在");
        }
        if (request.getExpressStationId() != null) {
            ExpressBillStationFilter stationFilter = ExpressBillStationFilter.parse(request.getExpressStationId());
            bill.setExpressStationId(stationFilter.billStationId());
            bill.setOtherExpress(stationFilter.billOtherExpressFlag());
            if (stationFilter.otherExpress()) {
                bill.setIncludeLabelPrice(0);
            }
            expressBillMapper.updateById(bill);
        }
        ExpressBillStationFilter stationFilter = ExpressBillStationFilter.fromBill(bill);
        EcExpressStation station = stationFilter.otherExpress()
                ? null : requireExpressStation(stationFilter.stationId());
        boolean addLabelPrice = Objects.equals(bill.getIncludeLabelPrice(), 1) && !stationFilter.otherExpress();
        Long lineExpressStationId = stationFilter.billStationId();
        int applied = 0;
        List<EcSettlementExpressBillManualSaveRequest.ManualLineItem> items =
                request.getLines() == null ? List.of() : request.getLines();

        for (EcSettlementExpressBillManualSaveRequest.ManualLineItem item : items) {
            EcSettlementExpressBillLine line;
            if (item.getLineId() != null) {
                line = expressBillLineMapper.selectById(item.getLineId());
                if (line == null || !Objects.equals(line.getBillId(), bill.getId())) {
                    continue;
                }
            } else {
                line = new EcSettlementExpressBillLine();
                line.setBillId(bill.getId());
                line.setSource("MANUAL");
                line.setMatchStatus("PENDING");
            }
            line.setExpressStationId(lineExpressStationId);
            line.setPlatformOrderNo(trimToNull(item.getPlatformOrderNo()));
            line.setOrderNo(trimToNull(item.getOrderNo()));
            String tracking = trimToNull(item.getTrackingNumber());
            line.setTrackingNumber(StringUtils.hasText(tracking)
                    ? EcExpressBillParseSupport.normalizeTracking(tracking) : null);
            BigDecimal inputFreight = item.getFreightAmount();
            BigDecimal appliedFreight = resolveManualFreight(inputFreight, line.getFreightAmount(), station, addLabelPrice);
            line.setFreightAmount(appliedFreight);
            line.setSettlementDestination(trimToNull(item.getSettlementDestination()));
            line.setWeight(item.getWeight());
            line.setShipTime(item.getShipTime());
            line.setRemark(trimToNull(item.getRemark()));

            if (item.getOrderId() != null) {
                line.setOrderId(item.getOrderId());
            } else {
                EcSalesOrder matched = resolveOrderForManualLine(line);
                if (matched != null) {
                    line.setOrderId(matched.getId());
                    if (!StringUtils.hasText(line.getOrderNo())) {
                        line.setOrderNo(matched.getOrderNo());
                    }
                    if (!StringUtils.hasText(line.getPlatformOrderNo())) {
                        line.setPlatformOrderNo(matched.getPlatformOrderNo());
                    }
                }
            }

            if (appliedFreight != null && line.getOrderId() != null) {
                EcSalesOrder order = ecSalesOrderMapper.selectById(line.getOrderId());
                if (order != null) {
                    applyFreightToOrders(List.of(order), appliedFreight, station, false);
                    line.setMatchStatus("APPLIED");
                    applied++;
                }
            } else if (!"APPLIED".equals(line.getMatchStatus())) {
                line.setMatchStatus("PENDING");
            }

            if (line.getId() == null) {
                expressBillLineMapper.insert(line);
            } else {
                expressBillLineMapper.updateById(line);
            }
        }

        bill.setManualAppliedRows(nvlInt(bill.getManualAppliedRows()) + applied);
        if (!"MIXED".equals(bill.getImportMode()) && bill.getTotalRows() != null && bill.getTotalRows() > 0) {
            bill.setImportMode("MIXED");
        }
        expressBillMapper.updateById(bill);

        EcSettlementExpressBillImportVO vo = toImportVO(bill,
                resolveBillStationName(bill, station != null ? station.getName() : null),
                countManualPendingOrders(bill.getBillMonth(), stationFilter));
        vo.setManualAppliedRows(bill.getManualAppliedRows());
        return vo;
    }

    @Override
    public List<EcSettlementExpressBillLineVO> listManualPendingLines(Long billId) {
        if (billId == null) {
            return List.of();
        }
        EcSettlementExpressBill bill = expressBillMapper.selectById(billId);
        if (bill == null) {
            return List.of();
        }
        ExpressBillStationFilter stationFilter = ExpressBillStationFilter.fromBill(bill);
        EcExpressStation station = bill.getExpressStationId() != null
                ? expressStationMapper.selectById(bill.getExpressStationId()) : null;

        List<EcSettlementExpressBillLine> savedLines = expressBillLineMapper.selectList(
                new LambdaQueryWrapper<EcSettlementExpressBillLine>()
                        .eq(EcSettlementExpressBillLine::getBillId, billId)
                        .in(EcSettlementExpressBillLine::getSource, "GAP_ORDER", "MANUAL")
                        .eq(EcSettlementExpressBillLine::getMatchStatus, "PENDING")
                        .orderByAsc(EcSettlementExpressBillLine::getId));

        Map<Long, EcSettlementExpressBillLine> lineByOrderId = savedLines.stream()
                .filter(line -> line.getOrderId() != null)
                .collect(Collectors.toMap(EcSettlementExpressBillLine::getOrderId, line -> line, (a, b) -> a));

        List<EcSalesOrder> gapOrders = loadOrdersWithoutFreight(bill.getBillMonth(), stationFilter);
        Map<Long, EcSalesOrder> orderMap = gapOrders.stream()
                .collect(Collectors.toMap(EcSalesOrder::getId, order -> order, (a, b) -> a));
        for (EcSettlementExpressBillLine saved : savedLines) {
            if (saved.getOrderId() != null && !orderMap.containsKey(saved.getOrderId())) {
                EcSalesOrder order = ecSalesOrderMapper.selectById(saved.getOrderId());
                if (order != null) {
                    orderMap.put(order.getId(), order);
                }
            }
        }
        Map<Long, String> shopNameMap = loadShopNameMap(orderMap.values().stream()
                .map(EcSalesOrder::getShopId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        List<EcSettlementExpressBillLineVO> result = new ArrayList<>();
        Set<Long> seenOrderIds = new HashSet<>();
        for (EcSalesOrder order : gapOrders) {
            seenOrderIds.add(order.getId());
            EcSettlementExpressBillLine line = lineByOrderId.get(order.getId());
            if (line != null) {
                result.add(buildManualLineVO(line, bill, station, orderMap, shopNameMap));
            } else {
                result.add(orderToLineVO(order, bill, shopNameMap));
            }
        }
        for (EcSettlementExpressBillLine line : savedLines) {
            if (line.getOrderId() == null || !seenOrderIds.contains(line.getOrderId())) {
                result.add(buildManualLineVO(line, bill, station, orderMap, shopNameMap));
            }
        }
        return result;
    }

    private EcSettlementExpressBillLineVO buildManualLineVO(EcSettlementExpressBillLine line,
                                                            EcSettlementExpressBill bill,
                                                            EcExpressStation station,
                                                            Map<Long, EcSalesOrder> orderMap,
                                                            Map<Long, String> shopNameMap) {
        EcSettlementExpressBillLineVO vo = toLineVO(line, bill, station);
        if (line.getOrderId() != null) {
            EcSalesOrder order = orderMap.get(line.getOrderId());
            if (order != null) {
                vo.setShipTime(order.getPayTime());
                if (order.getShopId() != null) {
                    vo.setShopName(shopNameMap.get(order.getShopId()));
                }
            }
        }
        return vo;
    }

    private EcSettlementExpressBillLineVO orderToLineVO(EcSalesOrder order, EcSettlementExpressBill bill,
                                                        Map<Long, String> shopNameMap) {
        EcSettlementExpressBillLineVO vo = new EcSettlementExpressBillLineVO();
        vo.setBillId(bill.getId());
        vo.setSource("GAP_ORDER");
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPlatformOrderNo(order.getPlatformOrderNo());
        vo.setTrackingNumber(order.getTrackingNumber() != null
                ? EcExpressBillParseSupport.normalizeTracking(order.getTrackingNumber()) : null);
        vo.setShipTime(order.getPayTime());
        vo.setSettlementDestination(resolveOrderDestination(order));
        vo.setMatchStatus("PENDING");
        if (order.getShopId() != null) {
            vo.setShopName(shopNameMap.get(order.getShopId()));
        }
        return vo;
    }

    @Override
    public List<EcSettlementExpressBillRecordVO> listExpressBillRecords(String billMonth) {
        if (!StringUtils.hasText(billMonth)) {
            return List.of();
        }
        List<EcSettlementExpressBill> bills = expressBillMapper.selectList(new LambdaQueryWrapper<EcSettlementExpressBill>()
                .eq(EcSettlementExpressBill::getBillMonth, billMonth.trim())
                .eq(EcSettlementExpressBill::getStatus, "IMPORTED")
                .orderByDesc(EcSettlementExpressBill::getCreateTime));
        Map<Long, String> stationNames = loadExpressStationNameMap(bills);
        return bills.stream().map(b -> toRecordVO(b, stationNames.get(b.getExpressStationId()))).collect(Collectors.toList());
    }

    @Override
    public EcSettlementExpressBillPreviewVO previewExpressBillColumns(MultipartFile file, Integer headerRow) {
        int header = headerRow == null || headerRow < 1 ? 1 : headerRow;
        EcSettlementExpressBillPreviewVO vo = new EcSettlementExpressBillPreviewVO();
        vo.setColumns(EcExpressBillParseSupport.readHeaderColumns(file, header));
        return vo;
    }

    private EcSettlementExpressBillImportVO toImportVO(EcSettlementExpressBill bill, String stationName,
                                                       int manualPending) {
        EcSettlementExpressBillImportVO vo = new EcSettlementExpressBillImportVO();
        vo.setBillId(bill.getId());
        vo.setBillMonth(bill.getBillMonth());
        vo.setOtherExpress(Objects.equals(bill.getOtherExpress(), 1));
        vo.setExpressStationId(Objects.equals(bill.getOtherExpress(), 1)
                ? ExpressBillStationFilter.OTHER : bill.getExpressStationId());
        vo.setExpressStationName(stationName);
        vo.setTotalRows(bill.getTotalRows());
        vo.setMatchedRows(bill.getMatchedRows());
        vo.setUnmatchedRows(bill.getUnmatchedRows());
        vo.setGapOrderRows(bill.getGapOrderRows());
        vo.setManualPendingRows(manualPending);
        return vo;
    }

    private EcSettlementExpressBillLineVO toLineVO(EcSettlementExpressBillLine line,
                                                   EcSettlementExpressBill bill,
                                                   EcExpressStation station) {
        EcSettlementExpressBillLineVO vo = new EcSettlementExpressBillLineVO();
        vo.setId(line.getId());
        vo.setBillId(line.getBillId());
        vo.setExpressStationId(line.getExpressStationId());
        vo.setSource(line.getSource());
        vo.setOrderId(line.getOrderId());
        vo.setPlatformOrderNo(line.getPlatformOrderNo());
        vo.setOrderNo(line.getOrderNo());
        vo.setTrackingNumber(line.getTrackingNumber());
        vo.setFreightAmount(line.getFreightAmount());
        vo.setSettlementDestination(line.getSettlementDestination());
        vo.setWeight(line.getWeight());
        vo.setShipTime(line.getShipTime());
        vo.setMatchStatus(line.getMatchStatus());
        vo.setRemark(line.getRemark());
        return vo;
    }

    private EcSettlementExpressBillRecordVO toRecordVO(EcSettlementExpressBill bill, String stationName) {
        EcSettlementExpressBillRecordVO vo = new EcSettlementExpressBillRecordVO();
        vo.setId(bill.getId());
        vo.setBillMonth(bill.getBillMonth());
        vo.setOtherExpress(Objects.equals(bill.getOtherExpress(), 1));
        vo.setExpressStationId(Objects.equals(bill.getOtherExpress(), 1)
                ? ExpressBillStationFilter.OTHER : bill.getExpressStationId());
        vo.setExpressStationName(resolveBillStationName(bill, stationName));
        vo.setFileName(bill.getFileName());
        vo.setImportMode(bill.getImportMode());
        vo.setTotalRows(bill.getTotalRows());
        vo.setMatchedRows(bill.getMatchedRows());
        vo.setUnmatchedRows(bill.getUnmatchedRows());
        vo.setGapOrderRows(bill.getGapOrderRows());
        vo.setManualAppliedRows(bill.getManualAppliedRows());
        vo.setIncludeLabelPrice(Objects.equals(bill.getIncludeLabelPrice(), 1));
        vo.setStatus(bill.getStatus());
        vo.setCreateTime(bill.getCreateTime());
        return vo;
    }

    private int countManualPendingOrders(String billMonth, ExpressBillStationFilter stationFilter) {
        return loadOrdersWithoutFreight(billMonth, stationFilter).size();
    }

    private List<EcSalesOrder> loadOrdersWithoutFreight(String billMonth, ExpressBillStationFilter stationFilter) {
        YearMonth ym = parseMonth(billMonth);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        LambdaQueryWrapper<EcSalesOrder> wrapper = new LambdaQueryWrapper<EcSalesOrder>()
                .ge(EcSalesOrder::getOrderTime, start)
                .lt(EcSalesOrder::getOrderTime, end)
                .in(EcSalesOrder::getStatus, AUTO_INCLUDE_STATUS)
                .and(w -> w.isNull(EcSalesOrder::getActualFreightAmount)
                        .or().eq(EcSalesOrder::getActualFreightAmount, BigDecimal.ZERO));
        applyExpressStationOrderFilter(wrapper, stationFilter);
        return ecSalesOrderMapper.selectList(wrapper.orderByAsc(EcSalesOrder::getOrderTime));
    }

    private void applyExpressStationOrderFilter(LambdaQueryWrapper<EcSalesOrder> wrapper,
                                                ExpressBillStationFilter stationFilter) {
        if (stationFilter.otherExpress()) {
            Set<Long> validStationIds = expressStationMapper.selectList(new LambdaQueryWrapper<>()).stream()
                    .map(EcExpressStation::getId)
                    .collect(Collectors.toSet());
            if (validStationIds.isEmpty()) {
                wrapper.isNull(EcSalesOrder::getExpressStationId);
            } else {
                wrapper.and(w -> w.isNull(EcSalesOrder::getExpressStationId)
                        .or().notIn(EcSalesOrder::getExpressStationId, validStationIds));
            }
            return;
        }
        if (stationFilter.stationId() != null) {
            wrapper.eq(EcSalesOrder::getExpressStationId, stationFilter.stationId());
        }
    }

    private String resolveBillStationName(EcSettlementExpressBill bill, String stationName) {
        if (Objects.equals(bill.getOtherExpress(), 1)) {
            return "其他快递公司";
        }
        return stationName;
    }

    private Map<String, EcSettlementExpressBillLine> loadExistingLinesByTracking(String billMonth,
                                                                               ExpressBillStationFilter stationFilter) {
        LambdaQueryWrapper<EcSettlementExpressBill> billWrapper = new LambdaQueryWrapper<EcSettlementExpressBill>()
                .eq(EcSettlementExpressBill::getBillMonth, parseMonth(billMonth).toString())
                .eq(EcSettlementExpressBill::getStatus, "IMPORTED");
        if (stationFilter.otherExpress()) {
            billWrapper.eq(EcSettlementExpressBill::getOtherExpress, 1);
        } else if (stationFilter.stationId() != null) {
            billWrapper.eq(EcSettlementExpressBill::getExpressStationId, stationFilter.stationId())
                    .and(w -> w.isNull(EcSettlementExpressBill::getOtherExpress)
                            .or().eq(EcSettlementExpressBill::getOtherExpress, 0));
        }
        List<EcSettlementExpressBill> bills = expressBillMapper.selectList(billWrapper);
        if (bills.isEmpty()) {
            return new HashMap<>();
        }
        Set<Long> billIds = bills.stream().map(EcSettlementExpressBill::getId).collect(Collectors.toSet());
        List<EcSettlementExpressBillLine> lines = expressBillLineMapper.selectList(
                new LambdaQueryWrapper<EcSettlementExpressBillLine>()
                        .in(EcSettlementExpressBillLine::getBillId, billIds)
                        .eq(EcSettlementExpressBillLine::getSource, "FILE")
                        .isNotNull(EcSettlementExpressBillLine::getTrackingNumber)
                        .orderByAsc(EcSettlementExpressBillLine::getId));
        Map<String, EcSettlementExpressBillLine> map = new LinkedHashMap<>();
        for (EcSettlementExpressBillLine line : lines) {
            String tracking = EcExpressBillParseSupport.normalizeTracking(line.getTrackingNumber());
            if (StringUtils.hasText(tracking)) {
                map.put(tracking, line);
            }
        }
        return map;
    }

    private int appendGapOrderLines(Long billId, String billMonth, ExpressBillStationFilter stationFilter,
                                    Set<Long> matchedOrderIds) {
        List<EcSalesOrder> orders = loadOrdersWithoutFreight(billMonth, stationFilter);
        Long lineExpressStationId = stationFilter.billStationId();
        int count = 0;
        for (EcSalesOrder order : orders) {
            if (matchedOrderIds.contains(order.getId())) {
                continue;
            }
            EcSettlementExpressBillLine line = new EcSettlementExpressBillLine();
            line.setBillId(billId);
            line.setExpressStationId(lineExpressStationId != null ? lineExpressStationId : order.getExpressStationId());
            line.setSource("GAP_ORDER");
            line.setOrderId(order.getId());
            line.setOrderNo(order.getOrderNo());
            line.setPlatformOrderNo(order.getPlatformOrderNo());
            line.setTrackingNumber(order.getTrackingNumber() != null
                    ? EcExpressBillParseSupport.normalizeTracking(order.getTrackingNumber()) : null);
            line.setShipTime(order.getPayTime());
            line.setSettlementDestination(resolveOrderDestination(order));
            line.setMatchStatus("PENDING");
            expressBillLineMapper.insert(line);
            count++;
        }
        return count;
    }

    private List<EcSalesOrder> findOrdersForBillRow(ExpressBillRow row) {
        return findOrdersByTracking(row.trackingNumber());
    }

    private List<EcSalesOrder> findOrdersByTracking(String tracking) {
        String normalized = EcExpressBillParseSupport.normalizeTracking(tracking);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        List<EcSalesOrder> exact = ecSalesOrderMapper.selectList(new LambdaQueryWrapper<EcSalesOrder>()
                .eq(EcSalesOrder::getTrackingNumber, normalized));
        if (!exact.isEmpty()) {
            return exact;
        }
        if (!normalized.equals(tracking)) {
            exact = ecSalesOrderMapper.selectList(new LambdaQueryWrapper<EcSalesOrder>()
                    .eq(EcSalesOrder::getTrackingNumber, tracking));
            if (!exact.isEmpty()) {
                return exact;
            }
        }
        return ecSalesOrderMapper.selectList(new LambdaQueryWrapper<EcSalesOrder>()
                .apply("REPLACE(tracking_number, ' ', '') = {0}", normalized));
    }

    private String resolveOrderDestination(EcSalesOrder order) {
        if (order == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(order.getReceiveProvince())) {
            sb.append(order.getReceiveProvince().trim());
        }
        if (StringUtils.hasText(order.getReceiveCity())) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(order.getReceiveCity().trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private EcSalesOrder resolveOrderForManualLine(EcSettlementExpressBillLine line) {
        if (StringUtils.hasText(line.getTrackingNumber())) {
            List<EcSalesOrder> orders = findOrdersByTracking(line.getTrackingNumber());
            if (!orders.isEmpty()) {
                return orders.get(0);
            }
        }
        if (StringUtils.hasText(line.getOrderNo())) {
            EcSalesOrder byOrderNo = ecSalesOrderMapper.selectOne(new LambdaQueryWrapper<EcSalesOrder>()
                    .eq(EcSalesOrder::getOrderNo, line.getOrderNo())
                    .last("LIMIT 1"));
            if (byOrderNo != null) {
                return byOrderNo;
            }
        }
        if (StringUtils.hasText(line.getPlatformOrderNo())) {
            EcSalesOrder byPlatform = ecSalesOrderMapper.selectOne(new LambdaQueryWrapper<EcSalesOrder>()
                    .eq(EcSalesOrder::getPlatformOrderNo, line.getPlatformOrderNo())
                    .last("LIMIT 1"));
            if (byPlatform != null) {
                return byPlatform;
            }
        }
        return null;
    }

    private void applyFreightToOrders(List<EcSalesOrder> orders, BigDecimal billFreight,
                                      EcExpressStation station, boolean includeLabelPrice) {
        BigDecimal applied = resolveAppliedFreight(billFreight, station, includeLabelPrice);
        for (EcSalesOrder order : orders) {
            order.setActualFreightAmount(applied);
            ecSalesOrderMapper.updateById(order);
        }
    }

    private BigDecimal resolveAppliedFreight(BigDecimal billFreight, EcExpressStation station,
                                             boolean includeLabelPrice) {
        BigDecimal base = nvl(billFreight);
        if (!includeLabelPrice || station == null || station.getLabelPrice() == null) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }
        return base.add(station.getLabelPrice()).setScale(2, RoundingMode.HALF_UP);
    }

    /** 手动保存：新填运费视为账单原始金额；已保存过的行按用户输入总额处理 */
    private BigDecimal resolveManualFreight(BigDecimal inputFreight, BigDecimal previousFreight,
                                            EcExpressStation station, boolean includeLabelPrice) {
        if (inputFreight == null) {
            return null;
        }
        if (!includeLabelPrice) {
            return inputFreight.setScale(2, RoundingMode.HALF_UP);
        }
        if (previousFreight != null && inputFreight.compareTo(previousFreight) == 0) {
            return inputFreight.setScale(2, RoundingMode.HALF_UP);
        }
        if (previousFreight == null || previousFreight.compareTo(BigDecimal.ZERO) == 0) {
            return resolveAppliedFreight(inputFreight, station, true);
        }
        return inputFreight.setScale(2, RoundingMode.HALF_UP);
    }

    private EcExpressStation requireExpressStation(Long expressStationId) {
        EcExpressStation station = expressStationMapper.selectById(expressStationId);
        if (station == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "快递公司不存在");
        }
        return station;
    }

    private Map<Long, String> loadExpressStationNameMap(List<EcSettlementExpressBill> bills) {
        Set<Long> ids = bills.stream()
                .map(EcSettlementExpressBill::getExpressStationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return expressStationMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(EcExpressStation::getId, EcExpressStation::getName, (a, b) -> a));
    }

    private Map<String, String> resolveColumnMapping(Long expressStationId, Map<String, String> columnMapping,
                                                     MultipartFile file, int header) {
        String bizType = SysImportFieldRegistry.BIZ_SETTLEMENT_EXPRESS_BILL;
        if (columnMapping != null && !columnMapping.isEmpty()) {
            columnMappingSupport.validateRequiredFields(bizType, columnMapping);
            return SysImportFieldRegistry.sanitizeColumnMapping(bizType, columnMapping);
        }
        SysImportProfileVO profile = sysImportService.getProfileByScope(
                bizType, SysImportColumnMappingSupport.expressStationScopeKey(expressStationId));
        if (profile.getColumnMapping() != null && hasExpressBillMapping(profile.getColumnMapping())) {
            return profile.getColumnMapping();
        }
        List<String> columns = EcExpressBillParseSupport.readHeaderColumns(file, header);
        Map<String, String> mapping = EcExpressBillParseSupport.defaultColumnMapping(columns);
        columnMappingSupport.validateRequiredFields(bizType, mapping);
        return mapping;
    }

    private boolean hasExpressBillMapping(Map<String, String> mapping) {
        return StringUtils.hasText(mapping.get("tracking_number"))
                && StringUtils.hasText(mapping.get("freight_amount"));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int nvlInt(Integer value) {
        return value == null ? 0 : value;
    }

    private EcMonthlySettlementVO.ShopSummary buildShopSummary(EcShop shop, Map<Long, String> shopNameMap,
                                                               LocalDateTime start, LocalDateTime end,
                                                               String settlementMonth) {
        Set<String> excludedBuyers = loadExcludedBuyerNames(shop.getId());
        Map<Long, EcSettlementOrderDecision> decisionMap = loadDecisionMap(shop.getId(), settlementMonth);
        Map<Long, List<EcSalesOrderLine>> lineMap = loadLineMapForShop(shop.getId(), start, end);

        List<EcSalesOrder> orders = ecSalesOrderMapper.selectList(new LambdaQueryWrapper<EcSalesOrder>()
                .eq(EcSalesOrder::getShopId, shop.getId())
                .ge(EcSalesOrder::getOrderTime, start)
                .lt(EcSalesOrder::getOrderTime, end)
                .orderByAsc(EcSalesOrder::getOrderTime));

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal estimatedTotalCost = BigDecimal.ZERO;
        BigDecimal actualTotalCost = BigDecimal.ZERO;
        int includedCount = 0;
        int excludedCount = 0;
        List<EcMonthlySettlementVO.PendingOrder> pendingOrders = new ArrayList<>();
        EcMonthlySettlementVO.MaxProfitOrder maxProfit = null;

        for (EcSalesOrder order : orders) {
            if (isBuyerExcluded(order.getBuyerName(), excludedBuyers)) {
                excludedCount++;
                continue;
            }
            List<EcSalesOrderLine> lines = lineMap.getOrDefault(order.getId(), List.of());
            SettlementAction action = resolveAction(order, lines, decisionMap.get(order.getId()));
            switch (action.type()) {
                case EXCLUDE -> excludedCount++;
                case PENDING -> pendingOrders.add(toPendingOrder(order, decisionMap.get(order.getId())));
                case INCLUDE_LOSS -> {
                    includedCount++;
                    BigDecimal cost = orderCost(order);
                    BigDecimal estFreight = nvl(order.getEstimatedFreightAmount());
                    BigDecimal actFreight = nvl(order.getActualFreightAmount());
                    estimatedTotalCost = estimatedTotalCost.add(cost).add(estFreight);
                    actualTotalCost = actualTotalCost.add(cost).add(actFreight);
                }
                case INCLUDE_PROFIT -> {
                    includedCount++;
                    BigDecimal received = nvl(order.getReceivedAmount());
                    BigDecimal cost = orderCost(order);
                    BigDecimal estFreight = nvl(order.getEstimatedFreightAmount());
                    BigDecimal actFreight = nvl(order.getActualFreightAmount());
                    totalRevenue = totalRevenue.add(received);
                    estimatedTotalCost = estimatedTotalCost.add(cost).add(estFreight);
                    actualTotalCost = actualTotalCost.add(cost).add(actFreight);
                    BigDecimal estProfit = received.subtract(cost).subtract(estFreight);
                    if (maxProfit == null || estProfit.compareTo(nvl(maxProfit.getProfitAmount())) > 0) {
                        maxProfit = new EcMonthlySettlementVO.MaxProfitOrder();
                        maxProfit.setOrderId(order.getId());
                        maxProfit.setOrderNo(order.getOrderNo());
                        maxProfit.setPlatformOrderNo(order.getPlatformOrderNo());
                        maxProfit.setProfitAmount(estProfit.setScale(2, RoundingMode.HALF_UP));
                        maxProfit.setReceivedAmount(received.setScale(2, RoundingMode.HALF_UP));
                    }
                }
            }
        }

        BigDecimal estimatedTotalProfit = totalRevenue.subtract(estimatedTotalCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualTotalProfit = totalRevenue.subtract(actualTotalCost).setScale(2, RoundingMode.HALF_UP);

        EcMonthlySettlementVO.ShopSummary summary = new EcMonthlySettlementVO.ShopSummary();
        summary.setShopId(shop.getId());
        summary.setShopName(shopNameMap.get(shop.getId()));
        summary.setTotalRevenue(scale2(totalRevenue));
        summary.setEstimatedTotalCost(scale2(estimatedTotalCost));
        summary.setActualTotalCost(scale2(actualTotalCost));
        summary.setEstimatedTotalProfit(estimatedTotalProfit);
        summary.setActualTotalProfit(actualTotalProfit);
        summary.setIncludedOrderCount(includedCount);
        summary.setExcludedOrderCount(excludedCount);
        summary.setPendingOrderCount(pendingOrders.size());
        summary.setMaxProfitOrder(maxProfit);
        summary.setPendingOrders(pendingOrders);
        return summary;
    }

    private SettlementAction resolveAction(EcSalesOrder order, List<EcSalesOrderLine> lines,
                                           EcSettlementOrderDecision decision) {
        if (hasReturnedLine(lines)) {
            return SettlementAction.includeLoss();
        }
        String status = normalizeStatus(order.getStatus());
        if (AUTO_EXCLUDE_STATUS.contains(status)) {
            return SettlementAction.exclude();
        }
        if (AUTO_INCLUDE_STATUS.contains(status)) {
            return SettlementAction.includeProfit();
        }
        if (PENDING_STATUS.contains(status)) {
            if (decision != null) {
                if (Objects.equals(decision.getIncluded(), 1)) {
                    return SettlementAction.includeProfit();
                }
                return SettlementAction.exclude();
            }
            return SettlementAction.pending();
        }
        if (decision != null && Objects.equals(decision.getIncluded(), 1)) {
            return SettlementAction.includeProfit();
        }
        return SettlementAction.exclude();
    }

    private boolean hasReturnedLine(List<EcSalesOrderLine> lines) {
        return lines.stream().anyMatch(l -> LINE_RETURNED.equals(normalizeStatus(l.getStatus())));
    }

    private EcMonthlySettlementVO.PendingOrder toPendingOrder(EcSalesOrder order, EcSettlementOrderDecision decision) {
        EcMonthlySettlementVO.PendingOrder row = new EcMonthlySettlementVO.PendingOrder();
        row.setOrderId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setPlatformOrderNo(order.getPlatformOrderNo());
        row.setStatus(order.getStatus());
        row.setBuyerName(order.getBuyerName());
        row.setReceivedAmount(order.getReceivedAmount());
        row.setOrderTime(order.getOrderTime());
        if (decision != null) {
            row.setDecided(true);
            row.setIncluded(Objects.equals(decision.getIncluded(), 1));
        } else {
            row.setDecided(false);
            row.setIncluded(null);
        }
        return row;
    }

    private BigDecimal orderCost(EcSalesOrder order) {
        return nvl(order.getTotalCostAmount());
    }

    private Set<String> loadExcludedBuyerNames(Long shopId) {
        List<EcSettlementBuyerExclude> list = buyerExcludeMapper.selectList(
                new LambdaQueryWrapper<EcSettlementBuyerExclude>()
                        .eq(EcSettlementBuyerExclude::getEnabled, 1)
                        .and(w -> w.eq(EcSettlementBuyerExclude::getShopId, shopId)
                                .or().isNull(EcSettlementBuyerExclude::getShopId)));
        Set<String> names = new HashSet<>();
        for (EcSettlementBuyerExclude item : list) {
            if (StringUtils.hasText(item.getBuyerName())) {
                names.add(item.getBuyerName().trim());
            }
        }
        return names;
    }

    private Map<Long, EcSettlementOrderDecision> loadDecisionMap(Long shopId, String month) {
        List<EcSettlementOrderDecision> list = orderDecisionMapper.selectList(
                new LambdaQueryWrapper<EcSettlementOrderDecision>()
                        .eq(EcSettlementOrderDecision::getShopId, shopId)
                        .eq(EcSettlementOrderDecision::getSettlementMonth, month));
        Map<Long, EcSettlementOrderDecision> map = new HashMap<>();
        for (EcSettlementOrderDecision item : list) {
            map.put(item.getOrderId(), item);
        }
        return map;
    }

    private Map<Long, List<EcSalesOrderLine>> loadLineMapForShop(Long shopId, LocalDateTime start, LocalDateTime end) {
        List<EcSalesOrder> orders = ecSalesOrderMapper.selectList(new LambdaQueryWrapper<EcSalesOrder>()
                .eq(EcSalesOrder::getShopId, shopId)
                .ge(EcSalesOrder::getOrderTime, start)
                .lt(EcSalesOrder::getOrderTime, end)
                .select(EcSalesOrder::getId));
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(EcSalesOrder::getId).toList();
        List<EcSalesOrderLine> lines = ecSalesOrderLineMapper.selectList(new LambdaQueryWrapper<EcSalesOrderLine>()
                .in(EcSalesOrderLine::getOrderId, orderIds));
        return lines.stream().collect(Collectors.groupingBy(EcSalesOrderLine::getOrderId));
    }

    private List<EcShop> loadShops(Long shopId) {
        if (shopId != null) {
            EcShop shop = ecShopMapper.selectById(shopId);
            if (shop == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            return List.of(shop);
        }
        return ecShopMapper.selectList(new LambdaQueryWrapper<EcShop>()
                .orderByAsc(EcShop::getName));
    }

    private Map<Long, String> loadShopNameMap(List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Map.of();
        }
        return ecShopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(EcShop::getId, EcShop::getName, (a, b) -> a));
    }

    private boolean isBuyerExcluded(String buyerName, Set<String> excluded) {
        if (!StringUtils.hasText(buyerName) || excluded.isEmpty()) {
            return false;
        }
        return excluded.contains(buyerName.trim());
    }

    private YearMonth parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请指定统计月份");
        }
        try {
            return YearMonth.parse(month.trim(), DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "月份格式应为 YYYY-MM");
        }
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale2(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record SettlementAction(SettlementType type) {
        static SettlementAction exclude() {
            return new SettlementAction(SettlementType.EXCLUDE);
        }

        static SettlementAction pending() {
            return new SettlementAction(SettlementType.PENDING);
        }

        static SettlementAction includeProfit() {
            return new SettlementAction(SettlementType.INCLUDE_PROFIT);
        }

        static SettlementAction includeLoss() {
            return new SettlementAction(SettlementType.INCLUDE_LOSS);
        }
    }

    private enum SettlementType {
        EXCLUDE, PENDING, INCLUDE_PROFIT, INCLUDE_LOSS
    }
}
