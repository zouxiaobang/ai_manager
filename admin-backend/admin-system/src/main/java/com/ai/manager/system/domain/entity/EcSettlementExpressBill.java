package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_settlement_express_bill")
public class EcSettlementExpressBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String billMonth;

    private Long expressStationId;

    /** 1=其他快递公司（订单未匹配系统快递站点） */
    private Integer otherExpress;

    /** 1=账单运费叠加站点面单价格 */
    private Integer includeLabelPrice;

    private String columnMapping;

    private Integer headerRow;

    private Integer dataStartRow;

    /** FILE / MANUAL / MIXED */
    private String importMode;

    private String fileName;

    private Integer totalRows;

    private Integer matchedRows;

    private Integer unmatchedRows;

    private Integer gapOrderRows;

    private Integer manualAppliedRows;

    private String status;

    private String errorMessage;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
