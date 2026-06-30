package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcFactorySaveRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.vo.EcFactoryStatsVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcFactoryService extends IService<EcFactory> {

    PageResult<EcFactory> pageFactories(String keyword, String factoryType, String status, Long page, Long pageSize);

    EcFactoryStatsVO getFactoryStats();

    List<EcFactory> listFactoryOptions(String factoryType);

    EcFactory createFactory(EcFactorySaveRequest request);

    EcFactory updateFactory(Long id, EcFactorySaveRequest request);

    void deleteFactory(Long id);
}
