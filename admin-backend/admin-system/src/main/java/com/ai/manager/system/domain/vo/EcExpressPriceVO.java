package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EcExpressPriceVO {

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

    private LocalDateTime updateTime;
}
