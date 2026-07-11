package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pixel_dog_item")
public class PixelDogItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String icon;
    private String color;
    private Integer requireLevel;
    private Integer sortOrder;
    private Integer shape;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
