package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcShopSaveRequest;
import com.ai.manager.system.domain.vo.EcShopListItemVO;
import com.ai.manager.system.service.EcShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/shops")
@RequiredArgsConstructor
public class EcShopController {

    private final EcShopService ecShopService;

    @GetMapping
    public ApiResult<PageResult<EcShopListItemVO>> list(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Long platformId,
                                                          @RequestParam(required = false) Long page,
                                                          @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecShopService.pageShops(keyword, platformId, page, pageSize));
    }

    @GetMapping("/options")
    public ApiResult<List<EcShopListItemVO>> options(@RequestParam(required = false) Long platformId) {
        return ApiResult.ok(ecShopService.listShopOptions(platformId));
    }

    @GetMapping("/{id}")
    public ApiResult<EcShopListItemVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecShopService.getShopDetail(id));
    }

    @PostMapping
    public ApiResult<EcShopListItemVO> create(@RequestBody EcShopSaveRequest request) {
        return ApiResult.ok(ecShopService.createShop(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcShopListItemVO> update(@PathVariable Long id, @RequestBody EcShopSaveRequest request) {
        return ApiResult.ok(ecShopService.updateShop(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecShopService.deleteShop(id);
        return ApiResult.ok();
    }
}
