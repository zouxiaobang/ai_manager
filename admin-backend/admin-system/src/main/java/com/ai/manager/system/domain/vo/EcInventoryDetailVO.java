package com.ai.manager.system.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EcInventoryDetailVO extends EcInventoryListItemVO {

    private Long skuId;

    private Long productId;

    private Long factoryId;

    private String factoryName;

    /** SKU 状态 ON_SALE/OFF_SALE/DRAFT */
    private String skuStatus;

    /** 草稿进货单在途数量 */
    private Integer inTransitQty;

    /** 关联进货单（含历史） */
    private List<EcInventoryInboundBriefVO> relatedInboundOrders;

    /** 关联出货单（含历史） */
    private List<EcInventoryOutboundBriefVO> relatedOutboundOrders;

    /** SKU 图片文件名 */
    private String imageName;

    /** 当前库存估箱（按账面数量） */
    private EcInventoryPackingEstimateVO packingEstimate;

    /** 指定出库数量的估箱 */
    private EcInventoryPackingEstimateVO outboundPackingEstimate;
}
