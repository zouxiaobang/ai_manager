package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcExpressStationSaveRequest;
import com.ai.manager.system.domain.vo.EcExpressStationDetailVO;
import com.ai.manager.system.service.EcExpressPriceService;
import com.ai.manager.system.service.EcExpressStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/express/stations")
@RequiredArgsConstructor
public class EcExpressStationController {

    private final EcExpressStationService ecExpressStationService;
    private final EcExpressPriceService ecExpressPriceService;

    @GetMapping
    public ApiResult<PageResult<EcExpressStationDetailVO>> list(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) Boolean defaultOnly,
                                                                @RequestParam(required = false) String regionNames,
                                                                @RequestParam(required = false) Long page,
                                                                @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecExpressStationService.pageStations(
                keyword, defaultOnly, parseRegionNames(regionNames), page, pageSize));
    }

    @GetMapping("/regions")
    public ApiResult<List<String>> listRegions() {
        return ApiResult.ok(ecExpressPriceService.listRegionNames());
    }

    @GetMapping("/{id}")
    public ApiResult<EcExpressStationDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecExpressStationService.getStationDetail(id));
    }

    @PostMapping
    public ApiResult<EcExpressStationDetailVO> create(@RequestBody EcExpressStationSaveRequest request) {
        return ApiResult.ok(ecExpressStationService.createStation(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcExpressStationDetailVO> update(@PathVariable Long id,
                                                      @RequestBody EcExpressStationSaveRequest request) {
        return ApiResult.ok(ecExpressStationService.updateStation(id, request));
    }

    @PostMapping("/{id}/copy")
    public ApiResult<EcExpressStationDetailVO> copy(@PathVariable Long id) {
        return ApiResult.ok(ecExpressStationService.copyStation(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecExpressStationService.deleteStation(id);
        return ApiResult.ok();
    }

    private List<String> parseRegionNames(String regionNames) {
        if (regionNames == null || regionNames.isBlank()) {
            return List.of();
        }
        return Arrays.stream(regionNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}
