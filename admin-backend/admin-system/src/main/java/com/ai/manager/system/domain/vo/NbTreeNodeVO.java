package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NbTreeNodeVO {

    private String nodeKey;

    private String nodeType;

    private Long notebookId;

    private Long noteId;

    private Long parentId;

    private String name;

    private Integer isPinned;

    private Integer isFavorite;

    private List<NbTreeNodeVO> children = new ArrayList<>();
}
