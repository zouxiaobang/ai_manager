package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcInventory;
import com.ai.manager.system.domain.entity.EcInventoryLog;
import com.ai.manager.system.domain.entity.EcSalesOrderInventoryDeduct;
import com.ai.manager.system.domain.entity.EcSalesOrderShortage;
import com.ai.manager.system.mapper.EcInventoryLogMapper;
import com.ai.manager.system.mapper.EcInventoryMapper;
import com.ai.manager.system.mapper.EcSalesOrderInventoryDeductMapper;
import com.ai.manager.system.mapper.EcSalesOrderShortageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EcSalesOrderInventorySupport {

    public static final String REF_SALES_ORDER = "SALES_ORDER";
    public static final String CHANGE_DEDUCT = "DEDUCT";
    public static final String SHORTAGE_OPEN = "OPEN";

    private final EcInventoryMapper ecInventoryMapper;
    private final EcInventoryLogMapper ecInventoryLogMapper;
    private final EcSalesOrderShortageMapper ecSalesOrderShortageMapper;
    private final EcSalesOrderInventoryDeductMapper ecSalesOrderInventoryDeductMapper;

    /**
     * 方案 B：扣至 0，不足记欠货。
     */
    public DeductOutcome deductSkuCode(Long orderId,
                                       Long orderLineId,
                                       String skuCode,
                                       int needQty) {
        EcInventory inventory = ecInventoryMapper.selectOne(new LambdaQueryWrapper<EcInventory>()
                .eq(EcInventory::getSkuCode, skuCode)
                .last("LIMIT 1"));
        int beforeQty = inventory != null ? inventory.getQuantity() : 0;
        int deductQty = Math.min(beforeQty, needQty);
        int shortQty = needQty - deductQty;
        int afterQty = beforeQty - deductQty;

        Long inventoryId = null;
        Long inventoryLogId = null;
        if (inventory != null && deductQty > 0) {
            inventory.setQuantity(afterQty);
            ecInventoryMapper.updateById(inventory);
            inventoryId = inventory.getId();
            EcInventoryLog log = new EcInventoryLog();
            log.setInventoryId(inventoryId);
            log.setChangeType(CHANGE_DEDUCT);
            log.setChangeQty(deductQty);
            log.setRefType(REF_SALES_ORDER);
            log.setRefId(orderId);
            log.setRemark("销售订单发货 lineId=" + orderLineId);
            ecInventoryLogMapper.insert(log);
            inventoryLogId = log.getId();
        }

        Long shortageId = null;
        if (shortQty > 0) {
            EcSalesOrderShortage shortage = new EcSalesOrderShortage();
            shortage.setOrderId(orderId);
            shortage.setOrderLineId(orderLineId);
            shortage.setSkuCode(skuCode);
            shortage.setNeedQty(needQty);
            shortage.setDeductedQty(deductQty);
            shortage.setShortQty(shortQty);
            shortage.setStatus(SHORTAGE_OPEN);
            shortage.setClearedQty(0);
            ecSalesOrderShortageMapper.insert(shortage);
            shortageId = shortage.getId();
        }

        if (deductQty > 0) {
            EcSalesOrderInventoryDeduct record = new EcSalesOrderInventoryDeduct();
            record.setOrderId(orderId);
            record.setOrderLineId(orderLineId);
            record.setShortageId(shortageId);
            record.setSkuCode(skuCode);
            record.setInventoryId(inventoryId);
            record.setInventoryLogId(inventoryLogId);
            record.setDeductQty(deductQty);
            record.setBeforeQty(beforeQty);
            record.setAfterQty(afterQty);
            ecSalesOrderInventoryDeductMapper.insert(record);
        }

        DeductOutcome outcome = new DeductOutcome();
        outcome.setNeedQty(needQty);
        outcome.setDeductedQty(deductQty);
        outcome.setShortQty(shortQty);
        outcome.setHasShortage(shortQty > 0);
        return outcome;
    }

    public boolean hasOrderInventoryDeduct(Long orderId) {
        if (orderId == null) {
            return false;
        }
        Long count = ecSalesOrderInventoryDeductMapper.selectCount(new LambdaQueryWrapper<EcSalesOrderInventoryDeduct>()
                .eq(EcSalesOrderInventoryDeduct::getOrderId, orderId));
        return count != null && count > 0;
    }

    @Data
    public static class DeductOutcome {
        private int needQty;
        private int deductedQty;
        private int shortQty;
        private boolean hasShortage;
    }
}
