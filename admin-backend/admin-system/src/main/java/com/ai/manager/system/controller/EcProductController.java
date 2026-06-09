package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcProductSaveRequest;
import com.ai.manager.system.domain.vo.EcProductDetailVO;
import com.ai.manager.system.domain.vo.EcProductListItemVO;
import com.ai.manager.system.service.EcProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/products")
@RequiredArgsConstructor
public class EcProductController {

    private final EcProductService ecProductService;

    @GetMapping
    public ApiResult<PageResult<EcProductListItemVO>> list(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Long page,
                                                           @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecProductService.pageProducts(keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResult<EcProductDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecProductService.getProductDetail(id));
    }

    @PostMapping
    public ApiResult<EcProductDetailVO> create(@RequestBody EcProductSaveRequest request) {
        return ApiResult.ok(ecProductService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcProductDetailVO> update(@PathVariable Long id, @RequestBody EcProductSaveRequest request) {
        return ApiResult.ok(ecProductService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecProductService.deleteProduct(id);
        return ApiResult.ok();
    }
}
