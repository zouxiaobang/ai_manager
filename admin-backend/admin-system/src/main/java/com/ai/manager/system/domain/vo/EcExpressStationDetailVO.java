package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcExpressStationDetailVO {

    private Long id;

    private String name;

    /** 站点头像（上传文件名） */
    private String avatarUrl;

    /** 各平台/导入文件中可能出现的名称别名，用于订单导入匹配 */
    private List<String> nameAliases;

    private String contact;

    private String address;

    private BigDecimal labelPrice;

    private Boolean isDefault;

    private LocalDateTime updateTime;

    private List<EcExpressPriceVO> prices;

    private List<EcExpressNoticeVO> notices;

    /** 价格矩阵行数（地区数） */
    private Integer priceCount;

    /** 注意事项条数 */
    private Integer noticeCount;
}
