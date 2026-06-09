package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcExpressStationSaveRequest;
import com.ai.manager.system.domain.vo.EcExpressStationDetailVO;
import com.ai.manager.system.service.EcExpressStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/express/stations")
@RequiredArgsConstructor
public class EcExpressStationController {

    private final EcExpressStationService ecExpressStationService;

    @GetMapping
    public ApiResult<PageResult<EcExpressStationDetailVO>> list(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) Long page,
                                                                @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecExpressStationService.pageStations(keyword, page, pageSize));
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

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecExpressStationService.deleteStation(id);
        return ApiResult.ok();
    }
}
