package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.EcExpressNoticeSaveRequest;
import com.ai.manager.system.domain.vo.EcExpressNoticeVO;
import com.ai.manager.system.service.EcExpressNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/express/notices")
@RequiredArgsConstructor
public class EcExpressNoticeController {

    private final EcExpressNoticeService ecExpressNoticeService;

    @GetMapping
    public ApiResult<List<EcExpressNoticeVO>> list(@RequestParam Long stationId) {
        return ApiResult.ok(ecExpressNoticeService.listNotices(stationId));
    }

    @PostMapping
    public ApiResult<EcExpressNoticeVO> create(@RequestBody EcExpressNoticeSaveRequest request) {
        return ApiResult.ok(ecExpressNoticeService.createNotice(request));
    }

    @PutMapping("/{id}")
    public ApiResult<EcExpressNoticeVO> update(@PathVariable Long id,
                                                @RequestBody EcExpressNoticeSaveRequest request) {
        return ApiResult.ok(ecExpressNoticeService.updateNotice(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecExpressNoticeService.deleteNotice(id);
        return ApiResult.ok();
    }
}
