package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.EcExpressPriceSaveRequest;
import com.ai.manager.system.domain.vo.EcExpressPriceVO;
import com.ai.manager.system.service.EcExpressPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/express/prices")
@RequiredArgsConstructor
public class EcExpressPriceController {

    private final EcExpressPriceService ecExpressPriceService;

    @GetMapping
    public ApiResult<List<EcExpressPriceVO>> list(@RequestParam Long stationId) {
        return ApiResult.ok(ecExpressPriceService.listPrices(stationId));
    }

    @PostMapping
    public ApiResult<EcExpressPriceVO> create(@RequestBody EcExpressPriceSaveRequest request) {
        return ApiResult.ok(ecExpressPriceService.createPrice(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcExpressPriceVO> update(@PathVariable Long id,
                                              @RequestBody EcExpressPriceSaveRequest request) {
        return ApiResult.ok(ecExpressPriceService.updatePrice(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecExpressPriceService.deletePrice(id);
        return ApiResult.ok();
    }
}
