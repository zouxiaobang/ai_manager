package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcOutboundOrderDetailVO {

    private Long id;

    private String orderNo;

    private Long factoryId;

    private String factoryName;

    private Long customerFactoryId;

    private String customerName;

    private String customerContactName;

    private String customerContactPhone;

    private String customerAddress;

    private String status;

    private String remark;

    private LocalDateTime orderTime;

    private LocalDateTime expectedShipTime;

    private LocalDateTime actualShipTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<EcOutboundOrderLineVO> lines;
}
