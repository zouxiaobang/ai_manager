package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.EcSettlementExpressBillManualSaveRequest;
import com.ai.manager.system.domain.dto.EcSettlementBuyerExcludeSaveRequest;
import com.ai.manager.system.domain.dto.EcSettlementOrderDecisionBatchRequest;
import com.ai.manager.system.domain.vo.EcMonthlySettlementVO;
import com.ai.manager.system.domain.vo.EcSettlementBuyerExcludeVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillImportVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillLineVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillPreviewVO;
import com.ai.manager.system.domain.vo.EcSettlementExpressBillRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface EcMonthlySettlementService {

    EcMonthlySettlementVO calculate(String settlementMonth, Long shopId);

    List<EcSettlementBuyerExcludeVO> listBuyerExcludes(Long shopId);

    EcSettlementBuyerExcludeVO saveBuyerExclude(EcSettlementBuyerExcludeSaveRequest request);

    void deleteBuyerExclude(Long id);

    EcMonthlySettlementVO saveOrderDecisions(EcSettlementOrderDecisionBatchRequest request);

    EcSettlementExpressBillImportVO importExpressBill(String billMonth, Long expressStationId, MultipartFile file,
                                                        Map<String, String> columnMapping, Integer headerRow,
                                                        Integer dataStartRow, Boolean includeLabelPrice);

    EcSettlementExpressBillImportVO prepareManualExpressBill(String billMonth, Long expressStationId,
                                                             Boolean includeLabelPrice);

    EcSettlementExpressBillImportVO saveManualExpressBillLines(EcSettlementExpressBillManualSaveRequest request);

    List<EcSettlementExpressBillLineVO> listManualPendingLines(Long billId);

    List<EcSettlementExpressBillRecordVO> listExpressBillRecords(String billMonth);

    EcSettlementExpressBillPreviewVO previewExpressBillColumns(MultipartFile file, Integer headerRow);

    boolean isExpressBillImported(String billMonth);
}
