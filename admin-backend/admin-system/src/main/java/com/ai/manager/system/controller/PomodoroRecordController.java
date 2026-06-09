package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.PomodoroRecordCreateRequest;
import com.ai.manager.system.domain.entity.PomodoroRecord;
import com.ai.manager.system.domain.vo.PomodoroDailyStatVO;
import com.ai.manager.system.domain.vo.PomodoroSummaryVO;
import com.ai.manager.system.service.PomodoroRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pomodoro")
@RequiredArgsConstructor
public class PomodoroRecordController {

    private final PomodoroRecordService pomodoroRecordService;

    @PostMapping("/records")
    public ApiResult<PomodoroRecord> createRecord(@RequestBody PomodoroRecordCreateRequest request) {
        return ApiResult.ok(pomodoroRecordService.createRecord(request));
    }

    @GetMapping("/records")
    public ApiResult<List<PomodoroRecord>> listRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.ok(pomodoroRecordService.listByDateRange(startDate, endDate));
    }

    @GetMapping("/stats/daily")
    public ApiResult<List<PomodoroDailyStatVO>> dailyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.ok(pomodoroRecordService.dailyStats(startDate, endDate));
    }

    @GetMapping("/stats/summary")
    public ApiResult<PomodoroSummaryVO> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.ok(pomodoroRecordService.summary(startDate, endDate));
    }

    @GetMapping("/stats/today")
    public ApiResult<Map<String, Object>> today() {
        LocalDate today = LocalDate.now();
        PomodoroSummaryVO summary = pomodoroRecordService.summary(today, today);
        Map<String, Object> data = new HashMap<>();
        data.put("workRounds", summary.getTotalWorkRounds());
        data.put("workMinutes", summary.getTotalWorkMinutes());
        data.put("breakMinutes", summary.getTotalBreakMinutes());
        return ApiResult.ok(data);
    }
}
