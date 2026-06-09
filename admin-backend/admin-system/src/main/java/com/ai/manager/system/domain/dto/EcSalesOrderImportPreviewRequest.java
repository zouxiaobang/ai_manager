package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EcSalesOrderImportPreviewRequest {

    private Long shopId;

    private Long profileId;

    private String fileName;

    private List<Map<String, String>> rows;
}
