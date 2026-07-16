package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DocLibraryTreeVO {

    private Long id;

    private Long parentId;

    private String name;

    private String icon;

    private String color;

    private Integer sortOrder;

    private List<DocLibraryTreeVO> children = new ArrayList<>();
}
