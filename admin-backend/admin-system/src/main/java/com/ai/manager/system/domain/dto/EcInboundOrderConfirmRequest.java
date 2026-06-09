package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class EcInboundOrderConfirmRequest {

    private List<EcInboundOrderConfirmLineItem> lines;
}
