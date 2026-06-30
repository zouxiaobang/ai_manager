package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcCartonSaveRequest {

    private String name;

    private Long factoryId;

    private BigDecimal lengthCm;

    private BigDecimal widthCm;

    private BigDecimal heightCm;

    private BigDecimal unitPrice;

    private String remark;

    /** 纸箱材质 0~3（牛皮/白卡/瓦楞/普通快递盒） */
    private Integer illustrationVariant;

    /** 3D 预览图文件名 */
    private String previewImage;
}
