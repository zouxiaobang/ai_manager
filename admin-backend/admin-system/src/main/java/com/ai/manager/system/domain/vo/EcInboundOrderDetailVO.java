package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcInboundOrderDetailVO {

    private Long id;

    private String orderNo;

    private Long factoryId;

    private String factoryName;

    private String status;

    private String remark;

    private LocalDateTime orderTime;

    private LocalDateTime expectedDeliveryTime;

    private LocalDateTime actualReceiptTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<EcInboundOrderLineVO> lines;
}
