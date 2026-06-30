package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcPurchaseOrderConfigVO {

  private String title;

  private String address;

  private String tel;

  private List<String> requirementItems;

  private List<String> noteItems;

  private String preparedBy;

  private String preparedPhone;

  private String receiverName;

  private String receiverPhone;

  private String receiverAddress;

  private String companyNo;

  private LocalDateTime updateTime;
}
