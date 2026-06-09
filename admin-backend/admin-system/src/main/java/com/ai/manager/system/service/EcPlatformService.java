package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcPlatformSaveRequest;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.vo.EcPlatformListItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcPlatformService extends IService<EcPlatform> {

    PageResult<EcPlatformListItemVO> pagePlatforms(String keyword, String channelType, Long page, Long pageSize);

    List<EcPlatformListItemVO> listPlatformOptions();

    EcPlatformListItemVO getPlatformDetail(Long id);

    EcPlatformListItemVO createPlatform(EcPlatformSaveRequest request);

    EcPlatformListItemVO updatePlatform(Long id, EcPlatformSaveRequest request);

    void deletePlatform(Long id);
}
