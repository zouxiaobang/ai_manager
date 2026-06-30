package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImageSpaceImageDetailVO {

    private String zone;

    private String fileName;

    private String relativePath;

    private long sizeBytes;

    private String modifiedAt;

    private int referenceCount;

    private List<String> linkedSpuNames = new ArrayList<>();

    private List<String> referenceHints = new ArrayList<>();
}
