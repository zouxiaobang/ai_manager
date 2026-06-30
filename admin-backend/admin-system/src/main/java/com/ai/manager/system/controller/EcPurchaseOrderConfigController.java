package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.EcPurchaseOrderConfigSaveRequest;
import com.ai.manager.system.domain.vo.EcPurchaseOrderConfigVO;
import com.ai.manager.system.service.EcPurchaseOrderConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/settings/purchase-order-config")
@RequiredArgsConstructor
public class EcPurchaseOrderConfigController {

  private final EcPurchaseOrderConfigService ecPurchaseOrderConfigService;

  @GetMapping
  public ApiResult<EcPurchaseOrderConfigVO> get() {
    return ApiResult.ok(ecPurchaseOrderConfigService.getConfig());
  }

  @PutMapping
  public ApiResult<EcPurchaseOrderConfigVO> save(@RequestBody EcPurchaseOrderConfigSaveRequest request) {
    return ApiResult.ok(ecPurchaseOrderConfigService.saveConfig(request));
  }
}
