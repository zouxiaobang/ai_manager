package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcListingLinkPricingRequest;
import com.ai.manager.system.domain.dto.EcListingLinkSaveRequest;
import com.ai.manager.system.domain.vo.EcListingLinkDetailVO;
import com.ai.manager.system.domain.vo.EcListingLinkPricingVO;
import com.ai.manager.system.service.EcListingLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecommerce/listing-links")
@RequiredArgsConstructor
public class EcListingLinkController {

    private final EcListingLinkService ecListingLinkService;

    @GetMapping
    public ApiResult<PageResult<EcListingLinkDetailVO>> list(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Long shopId,
                                                             @RequestParam(required = false) Long platformId,
                                                             @RequestParam(required = false) Long page,
                                                             @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecListingLinkService.pageLinks(keyword, shopId, platformId, page, pageSize));
    }

    @GetMapping("/by-product/{productId}")
    public ApiResult<List<EcListingLinkDetailVO>> listByProduct(@PathVariable Long productId) {
        return ApiResult.ok(ecListingLinkService.listLinksByProductId(productId));
    }

    @PostMapping("/calculate-pricing")
    public ApiResult<EcListingLinkPricingVO> calculatePricing(@RequestBody EcListingLinkPricingRequest request) {
        return ApiResult.ok(ecListingLinkService.calculatePricing(request));
    }

    @PostMapping("/recalculate-all")
    public ApiResult<Map<String, Integer>> recalculateAll() {
        int updated = ecListingLinkService.recalculateAllPricing();
        return ApiResult.ok(Map.of("updated", updated));
    }

    @GetMapping("/{id}")
    public ApiResult<EcListingLinkDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecListingLinkService.getLinkDetail(id));
    }

    @PostMapping
    public ApiResult<EcListingLinkDetailVO> create(@RequestBody EcListingLinkSaveRequest request) {
        return ApiResult.ok(ecListingLinkService.createLink(request));
    }

    @PostMapping("/{id}/copy")
    public ApiResult<EcListingLinkDetailVO> copy(@PathVariable Long id) {
        return ApiResult.ok(ecListingLinkService.copyLink(id));
    }

    @PutMapping("/{id}")
    public ApiResult<EcListingLinkDetailVO> update(@PathVariable Long id,
                                                   @RequestBody EcListingLinkSaveRequest request) {
        return ApiResult.ok(ecListingLinkService.updateLink(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecListingLinkService.deleteLink(id);
        return ApiResult.ok();
    }
}
