package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcPlatformSaveRequest;
import com.ai.manager.system.domain.vo.EcPlatformListItemVO;
import com.ai.manager.system.service.EcPlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/platforms")
@RequiredArgsConstructor
public class EcPlatformController {

    private final EcPlatformService ecPlatformService;

    @GetMapping
    public ApiResult<PageResult<EcPlatformListItemVO>> list(@RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String channelType,
                                                              @RequestParam(required = false) Long page,
                                                              @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecPlatformService.pagePlatforms(keyword, channelType, page, pageSize));
    }

    @GetMapping("/options")
    public ApiResult<List<EcPlatformListItemVO>> options() {
        return ApiResult.ok(ecPlatformService.listPlatformOptions());
    }

    @GetMapping("/{id}")
    public ApiResult<EcPlatformListItemVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecPlatformService.getPlatformDetail(id));
    }

    @PostMapping
    public ApiResult<EcPlatformListItemVO> create(@RequestBody EcPlatformSaveRequest request) {
        return ApiResult.ok(ecPlatformService.createPlatform(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcPlatformListItemVO> update(@PathVariable Long id,
                                                   @RequestBody EcPlatformSaveRequest request) {
        return ApiResult.ok(ecPlatformService.updatePlatform(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecPlatformService.deletePlatform(id);
        return ApiResult.ok();
    }
}
