package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.DailyChecklistSaveRequest;
import com.ai.manager.system.domain.vo.DailyChecklistStatsVO;
import com.ai.manager.system.domain.vo.DailyChecklistVO;

import java.time.LocalDate;
import java.util.List;

public interface DailyChecklistService {

    List<DailyChecklistVO> getByDate(LocalDate date);

    void saveByDate(DailyChecklistSaveRequest request);

    DailyChecklistStatsVO getStats(LocalDate startDate, LocalDate endDate);
}
