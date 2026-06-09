package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.SysImportProfileSaveRequest;
import com.ai.manager.system.domain.vo.SysImportFieldVO;
import com.ai.manager.system.domain.vo.SysImportProfileVO;
import com.ai.manager.system.service.SysImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sys/import")
@RequiredArgsConstructor
public class SysImportController {

    private final SysImportService sysImportService;

    @GetMapping("/fields")
    public ApiResult<List<SysImportFieldVO>> listFields(@RequestParam String bizType) {
        return ApiResult.ok(sysImportService.listFields(bizType));
    }

    @GetMapping("/profiles")
    public ApiResult<List<SysImportProfileVO>> listProfiles(@RequestParam String bizType,
                                                            @RequestParam(required = false) Long platformId,
                                                            @RequestParam(required = false) Long shopId,
                                                            @RequestParam(required = false) String scopeKey) {
        return ApiResult.ok(sysImportService.listProfiles(bizType, platformId, shopId, scopeKey));
    }

    @GetMapping("/profiles/by-scope")
    public ApiResult<SysImportProfileVO> getProfileByScope(@RequestParam String bizType,
                                                           @RequestParam String scopeKey) {
        return ApiResult.ok(sysImportService.getProfileByScope(bizType, scopeKey));
    }

    @GetMapping("/profiles/{id}")
    public ApiResult<SysImportProfileVO> getProfile(@PathVariable Long id) {
        return ApiResult.ok(sysImportService.getProfile(id));
    }

    @PostMapping("/profiles")
    public ApiResult<SysImportProfileVO> createProfile(@RequestBody SysImportProfileSaveRequest request) {
        request.setId(null);
        return ApiResult.ok(sysImportService.saveProfile(request));
    }

    @PutMapping("/profiles/{id}")
    public ApiResult<SysImportProfileVO> updateProfile(@PathVariable Long id,
                                                       @RequestBody SysImportProfileSaveRequest request) {
        request.setId(id);
        return ApiResult.ok(sysImportService.saveProfile(request));
    }
}
