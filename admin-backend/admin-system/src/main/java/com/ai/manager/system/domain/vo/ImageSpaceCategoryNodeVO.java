package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImageSpaceCategoryNodeVO {

    private String id;

    private String label;

    private Long spuId;

    private List<ImageSpaceCategoryNodeVO> children = new ArrayList<>();
}
