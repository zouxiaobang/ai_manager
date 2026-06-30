package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImageSpaceNormalizeResultVO {

    private boolean dryRun;

    private int planned;

    private int renamed;

    private int skipped;

    private int failed;

    private List<ImageSpaceNormalizeItemVO> items = new ArrayList<>();
}
