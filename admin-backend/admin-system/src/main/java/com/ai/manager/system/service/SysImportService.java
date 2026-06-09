package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.SysImportProfileSaveRequest;
import com.ai.manager.system.domain.vo.SysImportFieldVO;
import com.ai.manager.system.domain.vo.SysImportProfileVO;

import java.util.List;

public interface SysImportService {

    List<SysImportFieldVO> listFields(String bizType);

    List<SysImportProfileVO> listProfiles(String bizType, Long platformId, Long shopId, String scopeKey);

    SysImportProfileVO getProfile(Long id);

    SysImportProfileVO getProfileByScope(String bizType, String scopeKey);

    SysImportProfileVO saveProfile(SysImportProfileSaveRequest request);
}
