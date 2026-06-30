package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_carton")
public class EcCarton {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long factoryId;

    private String name;

    private BigDecimal lengthCm;

    private BigDecimal widthCm;

    private BigDecimal heightCm;

    private BigDecimal unitPrice;

    private String remark;

    /** 纸箱材质 0~3（牛皮/白卡/瓦楞/普通快递盒），null 时前端按 id 哈希选取 */
    private Integer illustrationVariant;

    /** 3D 预览图文件名（uploads/ecommerce 下） */
    private String previewImage;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
