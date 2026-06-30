package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_purchase_order_config")
public class EcPurchaseOrderConfig {

  public static final long SINGLETON_ID = 1L;

  @TableId
  private Long id;

  private String title;

  private String address;

  private String tel;

  /** JSON array of requirement strings */
  private String requirementItems;

  /** JSON array of note strings */
  private String noteItems;

  private String preparedBy;

  private String preparedPhone;

  private String receiverName;

  private String receiverPhone;

  private String receiverAddress;

  private String companyNo;

  private LocalDateTime updateTime;
}
