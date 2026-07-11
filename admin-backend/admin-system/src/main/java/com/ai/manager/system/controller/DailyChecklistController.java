package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.DailyChecklistSaveRequest;
import com.ai.manager.system.domain.vo.DailyChecklistStatsVO;
import com.ai.manager.system.domain.vo.DailyChecklistVO;
import com.ai.manager.system.service.DailyChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/24hour")
@RequiredArgsConstructor
public class DailyChecklistController {

    private final DailyChecklistService dailyChecklistService;

    @GetMapping
    public ApiResult<List<DailyChecklistVO>> getByDate(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return ApiResult.ok(dailyChecklistService.getByDate(localDate));
    }

    @PostMapping
    public ApiResult<Void> save(@RequestBody DailyChecklistSaveRequest request) {
        dailyChecklistService.saveByDate(request);
        return ApiResult.ok();
    }

    @GetMapping("/stats")
    public ApiResult<DailyChecklistStatsVO> getStats(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ApiResult.ok(dailyChecklistService.getStats(start, end));
    }
}
