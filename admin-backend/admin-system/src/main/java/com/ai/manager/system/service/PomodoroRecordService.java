package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.PomodoroRecordCreateRequest;
import com.ai.manager.system.domain.entity.PomodoroRecord;
import com.ai.manager.system.domain.vo.PomodoroDailyStatVO;
import com.ai.manager.system.domain.vo.PomodoroSummaryVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * 番茄钟记录服务接口
 *
 * <p>提供番茄钟记录的创建、日期范围查询、每日统计、汇总统计和今日工作轮次计数等番茄钟记录管理功能。</p>
 */
public interface PomodoroRecordService extends IService<PomodoroRecord> {

    /**
     * 创建番茄钟记录
     *
     * @param request 记录创建请求参数
     * @return 创建后的番茄钟记录
     */
    PomodoroRecord createRecord(PomodoroRecordCreateRequest request);

    /**
     * 根据日期范围查询番茄钟记录
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 番茄钟记录列表
     */
    List<PomodoroRecord> listByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 查询每日番茄钟统计
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 每日统计列表
     */
    List<PomodoroDailyStatVO> dailyStats(LocalDate startDate, LocalDate endDate);

    /**
     * 查询番茄钟汇总统计
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 汇总统计信息
     */
    PomodoroSummaryVO summary(LocalDate startDate, LocalDate endDate);

    /**
     * 统计今日工作轮次
     *
     * @return 今日工作轮次数
     */
    int countTodayWorkRounds();
}
