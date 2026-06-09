package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EcExpressStationSaveRequest {

    private String name;

    /** 导入文件中的快递名称别名（不含站点 canonical 名称） */
    private List<String> nameAliases;

    private String contact;

    private String address;

    private BigDecimal labelPrice;

    private Boolean isDefault;
}
