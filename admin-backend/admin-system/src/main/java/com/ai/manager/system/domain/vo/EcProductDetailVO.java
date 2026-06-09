package com.ai.manager.system.domain.vo;

import com.ai.manager.system.domain.entity.EcSku;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcProductDetailVO {

    private Long id;

    private Long factoryId;

    private String factoryName;

    private String name;

    private String description;

    private BigDecimal rebatePct;

    /** 图片文件名 */
    private String imageName;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<EcSku> skus;
}
