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

/**
 * 番茄钟记录控制器
 *
 * <p>所属模块：番茄钟模块-记录管理</p>
 * <p>API路径前缀：/api/pomodoro</p>
 * <p>功能描述：提供番茄钟记录的创建、查询、统计等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/pomodoro")
@RequiredArgsConstructor
public class PomodoroRecordController {

    private final PomodoroRecordService pomodoroRecordService;

    /**
     * 创建番茄钟记录
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/pomodoro/records</p>
     *
     * @param request 番茄钟记录创建请求参数
     * @return 创建后的番茄钟记录
     */
    @PostMapping("/records")
    public ApiResult<PomodoroRecord> createRecord(@RequestBody PomodoroRecordCreateRequest request) {
        return ApiResult.ok(pomodoroRecordService.createRecord(request));
    }

    /**
     * 查询番茄钟记录列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/records</p>
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 番茄钟记录列表
     */
    @GetMapping("/records")
    public ApiResult<List<PomodoroRecord>> listRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.ok(pomodoroRecordService.listByDateRange(startDate, endDate));
    }

    /**
     * 获取每日番茄钟统计
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/stats/daily</p>
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日统计列表
     */
    @GetMapping("/stats/daily")
    public ApiResult<List<PomodoroDailyStatVO>> dailyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.ok(pomodoroRecordService.dailyStats(startDate, endDate));
    }

    /**
     * 获取番茄钟汇总统计
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/stats/summary</p>
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 番茄钟汇总统计
     */
    @GetMapping("/stats/summary")
    public ApiResult<PomodoroSummaryVO> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.ok(pomodoroRecordService.summary(startDate, endDate));
    }

    /**
     * 获取今日番茄钟统计
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/stats/today</p>
     *
     * @return 今日番茄钟统计数据
     */
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
