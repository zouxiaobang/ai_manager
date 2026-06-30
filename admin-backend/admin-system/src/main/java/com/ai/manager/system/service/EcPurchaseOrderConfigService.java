package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.EcPurchaseOrderConfigSaveRequest;
import com.ai.manager.system.domain.entity.EcPurchaseOrderConfig;
import com.ai.manager.system.domain.vo.EcPurchaseOrderConfigVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface EcPurchaseOrderConfigService extends IService<EcPurchaseOrderConfig> {

  EcPurchaseOrderConfigVO getConfig();

  EcPurchaseOrderConfigVO saveConfig(EcPurchaseOrderConfigSaveRequest request);
}
