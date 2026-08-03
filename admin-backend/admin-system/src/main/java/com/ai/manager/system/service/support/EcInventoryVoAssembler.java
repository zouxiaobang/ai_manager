package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcInventory;
import com.ai.manager.system.domain.entity.EcInventoryLog;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.EcInventoryDetailVO;
import com.ai.manager.system.domain.vo.EcInventoryGlobalLogVO;
import com.ai.manager.system.domain.vo.EcInventoryInboundBriefVO;
import com.ai.manager.system.domain.vo.EcInventoryListItemVO;
import com.ai.manager.system.domain.vo.EcInventoryLogVO;
import com.ai.manager.system.domain.vo.EcInventoryOutboundBriefVO;
import com.ai.manager.system.domain.vo.EcInventoryPackingEstimateVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 电商库存 VO 组装器
 *
 * <p>从 {@code EcInventoryServiceImpl} 中提取的纯组装逻辑（只做对象 → VO 字段映射，无数据访问）。
 * 上下文（SkuContext/SkuBrief）与在途量/相关订单由服务层加载后作为参数传入，便于独立单元测试，
 * 也让库存服务主类保持职责单一。无状态，由 Spring 以单例注入。</p>
 */
@Component
public class EcInventoryVoAssembler {

    private static final int RECENT_LOG_LIMIT = 5;
    private static final String SKU_ON_SALE = "ON_SALE";
    private static final String PRODUCT_ENABLED = "ENABLED";

    /** SKU 简讯：列表行展示所需的商品维度摘要（服务层加载后传入） */
    public static final class SkuBrief {
        public String specName;
        public String productName;
        public Long productId;
        public String imageName;
        public BigDecimal salePrice;
    }

    /** SKU 上下文：详情/装箱估算所需的 SKU、商品、工厂、纸箱维度快照（服务层加载后传入） */
    public static final class SkuContext {
        public Long skuId;
        public Long productId;
        public Long factoryId;
        public String factoryName;
        public String specName;
        public String productName;
        public String skuStatus;
        public String productStatus;
        public BigDecimal salePrice;
        public String imageName;
        public Integer unitsPerCarton;
        public Long cartonId;
        public String cartonName;
        public BigDecimal cartonLengthCm;
        public BigDecimal cartonWidthCm;
        public BigDecimal cartonHeightCm;
    }

    /** 列表行 VO：规格/商品摘要 + 预警态 + 近期日志（超 5 条截断） */
    public EcInventoryListItemVO toListItemVO(EcInventory inventory, SkuBrief brief, List<EcInventoryLogVO> logs) {
        EcInventoryListItemVO vo = new EcInventoryListItemVO();
        vo.setId(inventory.getId());
        vo.setSkuCode(inventory.getSkuCode());
        if (brief != null) {
            vo.setSpecName(brief.specName);
            vo.setProductName(brief.productName);
            vo.setProductId(brief.productId);
            vo.setSalePrice(brief.salePrice);
            vo.setImageName(brief.imageName);
        }
        vo.setQuantity(inventory.getQuantity());
        vo.setIgnoreAlert(inventory.getIgnoreAlert() != null && inventory.getIgnoreAlert() == 1);
        vo.setAlertThreshold(inventory.getAlertThreshold());
        vo.setAlertActive(isAlertActive(inventory));
        vo.setUpdateTime(inventory.getUpdateTime());
        if (logs.size() > RECENT_LOG_LIMIT) {
            vo.setRecentLogs(logs.subList(0, RECENT_LOG_LIMIT));
        } else {
            vo.setRecentLogs(logs);
        }
        return vo;
    }

    /** 日志 VO：逐字段映射，去除实体内部字段 */
    public EcInventoryLogVO toLogVO(EcInventoryLog log) {
        EcInventoryLogVO vo = new EcInventoryLogVO();
        vo.setId(log.getId());
        vo.setInventoryId(log.getInventoryId());
        vo.setChangeType(log.getChangeType());
        vo.setChangeQty(log.getChangeQty());
        vo.setRefType(log.getRefType());
        vo.setRefId(log.getRefId());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    /** 全局日志 VO：日志 + 所属库存货号 + SKU 上下文摘要 */
    public EcInventoryGlobalLogVO toGlobalLogVO(EcInventoryLog log, EcInventory inventory, SkuContext ctx) {
        EcInventoryGlobalLogVO vo = new EcInventoryGlobalLogVO();
        vo.setId(log.getId());
        vo.setInventoryId(log.getInventoryId());
        vo.setChangeType(log.getChangeType());
        vo.setChangeQty(log.getChangeQty());
        vo.setRefType(log.getRefType());
        vo.setRefId(log.getRefId());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        if (inventory != null) {
            vo.setSkuCode(inventory.getSkuCode());
        }
        if (ctx != null) {
            vo.setSpecName(ctx.specName);
            vo.setProductName(ctx.productName);
            vo.setFactoryId(ctx.factoryId);
            vo.setFactoryName(ctx.factoryName);
        }
        return vo;
    }

    /**
     * 详情 VO：SKU 上下文摘要 + 在途量/相关出入库订单（服务层加载后传入）+ 装箱估算。
     */
    public EcInventoryDetailVO toDetailVO(EcInventory inventory, SkuContext ctx, List<EcInventoryLogVO> recentLogs,
                                          int inTransitQty, List<EcInventoryInboundBriefVO> relatedInboundOrders,
                                          List<EcInventoryOutboundBriefVO> relatedOutboundOrders) {
        EcInventoryDetailVO vo = new EcInventoryDetailVO();
        vo.setId(inventory.getId());
        vo.setSkuCode(inventory.getSkuCode());
        if (ctx != null) {
            vo.setSpecName(ctx.specName);
            vo.setProductName(ctx.productName);
            vo.setSalePrice(ctx.salePrice);
            vo.setSkuId(ctx.skuId);
            vo.setProductId(ctx.productId);
            vo.setFactoryId(ctx.factoryId);
            vo.setFactoryName(ctx.factoryName);
            vo.setSkuStatus(ctx.skuStatus);
        }
        vo.setQuantity(inventory.getQuantity());
        vo.setIgnoreAlert(inventory.getIgnoreAlert() != null && inventory.getIgnoreAlert() == 1);
        vo.setAlertThreshold(inventory.getAlertThreshold());
        vo.setAlertActive(isAlertActive(inventory));
        vo.setUpdateTime(inventory.getUpdateTime());
        vo.setRecentLogs(recentLogs);
        vo.setInTransitQty(inTransitQty);
        vo.setRelatedInboundOrders(relatedInboundOrders);
        vo.setRelatedOutboundOrders(relatedOutboundOrders);
        int qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        if (ctx != null) {
            vo.setImageName(ctx.imageName);
            vo.setPackingEstimate(buildPackingEstimate(ctx, qty));
            vo.setOutboundPackingEstimate(buildPackingEstimate(ctx, qty));
        }
        return vo;
    }

    /** 预警是否激活：忽略预警标记下永不激活，否则当前库存 ≤ 预警阈值 */
    public boolean isAlertActive(EcInventory inventory) {
        if (inventory.getIgnoreAlert() != null && inventory.getIgnoreAlert() == 1) {
            return false;
        }
        int threshold = inventory.getAlertThreshold() != null ? inventory.getAlertThreshold() : 0;
        int quantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        return quantity <= threshold;
    }

    /** 装箱估算：按每箱数量向上取整箱数，体积 = 单箱体积 × 箱数 */
    public EcInventoryPackingEstimateVO buildPackingEstimate(SkuContext ctx, int outboundQty) {
        EcInventoryPackingEstimateVO vo = new EcInventoryPackingEstimateVO();
        vo.setOutboundQty(outboundQty);
        int unitsPerCarton = ctx.unitsPerCarton != null && ctx.unitsPerCarton > 0 ? ctx.unitsPerCarton : 1;
        vo.setUnitsPerCarton(unitsPerCarton);
        vo.setCartonId(ctx.cartonId);
        vo.setCartonName(ctx.cartonName);
        if (outboundQty <= 0) {
            vo.setCartonsNeeded(0);
            vo.setCartonVolumeCm3(calcVolume(ctx.cartonLengthCm, ctx.cartonWidthCm, ctx.cartonHeightCm));
            vo.setTotalVolumeCm3(BigDecimal.ZERO);
            return vo;
        }
        int cartonsNeeded = (outboundQty + unitsPerCarton - 1) / unitsPerCarton;
        vo.setCartonsNeeded(cartonsNeeded);
        BigDecimal cartonVolume = calcVolume(ctx.cartonLengthCm, ctx.cartonWidthCm, ctx.cartonHeightCm);
        vo.setCartonVolumeCm3(cartonVolume);
        if (cartonVolume != null) {
            vo.setTotalVolumeCm3(cartonVolume.multiply(BigDecimal.valueOf(cartonsNeeded)));
        }
        return vo;
    }

    /** 纸箱体积：任一维度缺失返回 null（不参与估算） */
    public BigDecimal calcVolume(BigDecimal length, BigDecimal width, BigDecimal height) {
        if (length == null || width == null || height == null) {
            return null;
        }
        return length.multiply(width).multiply(height).setScale(2, RoundingMode.HALF_UP);
    }

    /** 是否允许进货：SKU 在售且所属商品已启用 */
    public boolean isSkuAvailableForInbound(EcSku sku, EcProduct product) {
        if (sku == null) {
            return false;
        }
        if (!SKU_ON_SALE.equals(sku.getStatus())) {
            return false;
        }
        if (product == null) {
            return false;
        }
        return PRODUCT_ENABLED.equals(product.getStatus());
    }
}
