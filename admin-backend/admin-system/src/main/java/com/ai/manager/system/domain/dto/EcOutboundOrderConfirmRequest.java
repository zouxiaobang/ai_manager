package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class EcOutboundOrderConfirmRequest {

    private List<EcOutboundOrderConfirmLineItem> lines;
}
