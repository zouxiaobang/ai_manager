package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcFactorySaveRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.service.EcFactoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/factories")
@RequiredArgsConstructor
public class EcFactoryController {

    private final EcFactoryService ecFactoryService;

    @GetMapping
    public ApiResult<PageResult<EcFactory>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecFactoryService.pageFactories(keyword, page, pageSize));
    }

    @GetMapping("/options")
    public ApiResult<List<EcFactory>> options() {
        return ApiResult.ok(ecFactoryService.listFactoryOptions());
    }

    @GetMapping("/{id}")
    public ApiResult<EcFactory> get(@PathVariable Long id) {
        EcFactory factory = ecFactoryService.getById(id);
        if (factory == null) {
            return ApiResult.fail(com.ai.manager.common.result.ResultCode.NOT_FOUND);
        }
        return ApiResult.ok(factory);
    }

    @PostMapping
    public ApiResult<EcFactory> create(@RequestBody EcFactorySaveRequest request) {
        return ApiResult.ok(ecFactoryService.createFactory(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcFactory> update(@PathVariable Long id, @RequestBody EcFactorySaveRequest request) {
        return ApiResult.ok(ecFactoryService.updateFactory(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecFactoryService.deleteFactory(id);
        return ApiResult.ok();
    }
}
