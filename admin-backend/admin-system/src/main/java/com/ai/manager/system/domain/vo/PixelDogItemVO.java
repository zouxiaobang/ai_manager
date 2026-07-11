package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class PixelDogItemVO {
    private Long id;
    private String name;
    private String icon;
    private String color;
    private Integer requireLevel;
    private Integer sortOrder;
    private Integer shape;
}
