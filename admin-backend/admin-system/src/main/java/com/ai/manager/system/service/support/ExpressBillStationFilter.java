package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.EcSettlementExpressBill;

import java.util.Objects;

/** 快递账单快递公司筛选：具体站点 ID，或「其他快递公司」(0) */
public final class ExpressBillStationFilter {

    public static final long OTHER = 0L;

    private final Long stationId;

    private final boolean otherExpress;

    private ExpressBillStationFilter(Long stationId, boolean otherExpress) {
        this.stationId = stationId;
        this.otherExpress = otherExpress;
    }

    public static ExpressBillStationFilter parse(Long expressStationId) {
        if (expressStationId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择快递公司");
        }
        if (expressStationId == OTHER) {
            return new ExpressBillStationFilter(null, true);
        }
        return new ExpressBillStationFilter(expressStationId, false);
    }

    public static ExpressBillStationFilter fromBill(EcSettlementExpressBill bill) {
        if (bill == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "账单批次不存在");
        }
        if (Objects.equals(bill.getOtherExpress(), 1)) {
            return new ExpressBillStationFilter(null, true);
        }
        return new ExpressBillStationFilter(bill.getExpressStationId(), false);
    }

    public Long stationId() {
        return stationId;
    }

    public boolean otherExpress() {
        return otherExpress;
    }

    /** 写入账单头：具体站点 ID，其他快递公司时为 null */
    public Long billStationId() {
        return otherExpress ? null : stationId;
    }

    public int billOtherExpressFlag() {
        return otherExpress ? 1 : 0;
    }
}
