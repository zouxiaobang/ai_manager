package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.PomodoroRecordCreateRequest;
import com.ai.manager.system.domain.entity.PomodoroRecord;
import com.ai.manager.system.domain.vo.PomodoroDailyStatVO;
import com.ai.manager.system.domain.vo.PomodoroSummaryVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface PomodoroRecordService extends IService<PomodoroRecord> {

    PomodoroRecord createRecord(PomodoroRecordCreateRequest request);

    List<PomodoroRecord> listByDateRange(LocalDate startDate, LocalDate endDate);

    List<PomodoroDailyStatVO> dailyStats(LocalDate startDate, LocalDate endDate);

    PomodoroSummaryVO summary(LocalDate startDate, LocalDate endDate);

    int countTodayWorkRounds();
}
