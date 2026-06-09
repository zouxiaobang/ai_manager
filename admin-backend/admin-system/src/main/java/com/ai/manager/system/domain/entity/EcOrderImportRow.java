package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_order_import_row")
public class EcOrderImportRow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private Integer rowNo;

    private String parseStatus;

    private String platformOrderNo;

    private String linkName;

    private String skuSpecName;

    private String matchStatus;

    private Long listingLinkSkuId;

    /** 未匹配链接时的手动成本（元/套） */
    private BigDecimal manualCostPrice;

    /** Excel 平台子订单/退款状态原文 */
    private String platformLineStatus;

    /** 解析或人工指定的系统行状态 */
    private String lineStatus;

    /** 状态映射 MATCHED/UNMATCHED */
    private String statusMatchStatus;

    private Long salesOrderId;

    private Long salesOrderLineId;

    private String errorMessage;

    private String rawJson;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
