package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_express_price")
public class EcExpressPrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

    private String provinceName;

    private BigDecimal priceW03Kg;

    private BigDecimal priceW05Kg;

    private BigDecimal priceW1Kg;

    private BigDecimal priceW15Kg;

    private BigDecimal priceW2Kg;

    private BigDecimal priceW25Kg;

    private BigDecimal priceW3Kg;

    private BigDecimal over3FirstPrice;

    private BigDecimal over3AdditionalPrice;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
