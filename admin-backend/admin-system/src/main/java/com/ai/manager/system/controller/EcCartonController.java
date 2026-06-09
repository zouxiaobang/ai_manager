package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcCartonSaveRequest;
import com.ai.manager.system.domain.vo.EcCartonBackfillTaskVO;
import com.ai.manager.system.domain.vo.EcCartonCalculateResultVO;
import com.ai.manager.system.domain.vo.EcCartonListItemVO;
import com.ai.manager.system.service.EcCartonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/cartons")
@RequiredArgsConstructor
public class EcCartonController {

    private final EcCartonService ecCartonService;

    @GetMapping
    public ApiResult<PageResult<EcCartonListItemVO>> list(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Long page,
                                                          @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecCartonService.pageCartons(keyword, page, pageSize));
    }

    @PostMapping("/backfill-sku-cartons")
    public ApiResult<Integer> backfillSkuCartons() {
        return ApiResult.ok(ecCartonService.backfillSkuCartons());
    }

    @PostMapping("/backfill-sku-cartons/async")
    public ApiResult<String> startBackfillSkuCartonsAsync() {
        return ApiResult.ok(ecCartonService.startBackfillSkuCartonsAsync());
    }

    @GetMapping("/backfill-sku-cartons/tasks/{taskId}")
    public ApiResult<EcCartonBackfillTaskVO> getBackfillTask(@PathVariable String taskId) {
        return ApiResult.ok(ecCartonService.getBackfillTask(taskId));
    }

    @GetMapping("/calculate")
    public ApiResult<EcCartonCalculateResultVO> calculate(@RequestParam java.math.BigDecimal lengthCm,
                                                          @RequestParam java.math.BigDecimal widthCm,
                                                          @RequestParam java.math.BigDecimal heightCm,
                                                          @RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecCartonService.calculateCartons(lengthCm, widthCm, heightCm, factoryId));
    }

    @GetMapping("/match")
    public ApiResult<EcCartonListItemVO> match(@RequestParam java.math.BigDecimal lengthCm,
                                               @RequestParam java.math.BigDecimal widthCm,
                                               @RequestParam java.math.BigDecimal heightCm,
                                               @RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecCartonService.matchCarton(lengthCm, widthCm, heightCm, factoryId));
    }

    @GetMapping("/{id}")
    public ApiResult<EcCartonListItemVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecCartonService.getCartonDetail(id));
    }

    @PostMapping
    public ApiResult<EcCartonListItemVO> create(@RequestBody EcCartonSaveRequest request) {
        return ApiResult.ok(ecCartonService.createCarton(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcCartonListItemVO> update(@PathVariable Long id, @RequestBody EcCartonSaveRequest request) {
        return ApiResult.ok(ecCartonService.updateCarton(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecCartonService.deleteCarton(id);
        return ApiResult.ok();
    }
}
