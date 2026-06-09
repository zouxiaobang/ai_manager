package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcFactorySaveRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcFactoryService extends IService<EcFactory> {

    PageResult<EcFactory> pageFactories(String keyword, Long page, Long pageSize);

    List<EcFactory> listFactoryOptions();

    EcFactory createFactory(EcFactorySaveRequest request);

    EcFactory updateFactory(Long id, EcFactorySaveRequest request);

    void deleteFactory(Long id);
}
