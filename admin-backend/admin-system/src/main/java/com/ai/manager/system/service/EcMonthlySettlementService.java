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

/**
 * 电商月度结算服务接口
 *
 * <p>提供电商月度结算的计算、快照加载、计算并保存、买家排除管理、订单决策保存、
 * 快递账单导入、手动快递账单管理、快递账单记录查询和列预览等月度结算相关功能。</p>
 */
public interface EcMonthlySettlementService {

    /**
     * 计算月度结算
     *
     * @param settlementMonth 结算月份
     * @param shopId          店铺ID
     * @return 月度结算数据
     */
    EcMonthlySettlementVO calculate(String settlementMonth, Long shopId);

    /**
     * 加载结算快照
     *
     * @param settlementMonth 结算月份
     * @return 月度结算数据
     */
    EcMonthlySettlementVO loadSnapshot(String settlementMonth);

    /**
     * 计算并保存月度结算
     *
     * @param settlementMonth 结算月份
     * @return 月度结算数据
     */
    EcMonthlySettlementVO calculateAndSave(String settlementMonth);

    /**
     * 查询买家排除列表
     *
     * @param shopId 店铺ID
     * @return 买家排除列表
     */
    List<EcSettlementBuyerExcludeVO> listBuyerExcludes(Long shopId);

    /**
     * 保存买家排除
     *
     * @param request 买家排除保存请求参数
     * @return 保存后的买家排除信息
     */
    EcSettlementBuyerExcludeVO saveBuyerExclude(EcSettlementBuyerExcludeSaveRequest request);

    /**
     * 删除买家排除
     *
     * @param id 排除记录ID
     */
    void deleteBuyerExclude(Long id);

    /**
     * 保存订单决策
     *
     * @param request 订单决策批量请求参数
     * @return 月度结算数据
     */
    EcMonthlySettlementVO saveOrderDecisions(EcSettlementOrderDecisionBatchRequest request);

    /**
     * 导入快递账单
     *
     * @param billMonth          账单月份
     * @param expressStationId   快递站点ID
     * @param file               导入文件
     * @param columnMapping      列映射
     * @param headerRow          表头行号
     * @param dataStartRow       数据起始行号
     * @param includeLabelPrice  是否包含面单价格
     * @return 快递账单导入结果
     */
    EcSettlementExpressBillImportVO importExpressBill(String billMonth, Long expressStationId, MultipartFile file,
                                                        Map<String, String> columnMapping, Integer headerRow,
                                                        Integer dataStartRow, Boolean includeLabelPrice);

    /**
     * 准备手动快递账单
     *
     * @param billMonth         账单月份
     * @param expressStationId  快递站点ID
     * @param includeLabelPrice 是否包含面单价格
     * @return 快递账单导入结果
     */
    EcSettlementExpressBillImportVO prepareManualExpressBill(String billMonth, Long expressStationId,
                                                             Boolean includeLabelPrice);

    /**
     * 保存手动快递账单行
     *
     * @param request 手动快递账单保存请求参数
     * @return 快递账单导入结果
     */
    EcSettlementExpressBillImportVO saveManualExpressBillLines(EcSettlementExpressBillManualSaveRequest request);

    /**
     * 查询手动待处理账单行列表
     *
     * @param billId 账单ID
     * @return 手动待处理账单行列表
     */
    List<EcSettlementExpressBillLineVO> listManualPendingLines(Long billId);

    /**
     * 查询未匹配的快递账单行列表
     *
     * @param billId 账单ID
     * @return 未匹配的快递账单行列表
     */
    List<EcSettlementExpressBillLineVO> listUnmatchedExpressBillLines(Long billId);

    /**
     * 查询快递账单记录列表
     *
     * @param billMonth 账单月份
     * @return 快递账单记录列表
     */
    List<EcSettlementExpressBillRecordVO> listExpressBillRecords(String billMonth);

    /**
     * 预览快递账单列
     *
     * @param file      文件
     * @param headerRow 表头行号
     * @return 快递账单列预览结果
     */
    EcSettlementExpressBillPreviewVO previewExpressBillColumns(MultipartFile file, Integer headerRow);

    /**
     * 判断快递账单是否已导入
     *
     * @param billMonth 账单月份
     * @return 是否已导入
     */
    boolean isExpressBillImported(String billMonth);
}
