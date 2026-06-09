package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.PomodoroRecord;
import com.ai.manager.system.domain.vo.PomodoroDailyStatVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PomodoroRecordMapper extends BaseMapper<PomodoroRecord> {

    @Select("""
            SELECT stat_date AS statDate,
                   SUM(CASE WHEN record_type = 'WORK' THEN 1 ELSE 0 END) AS workRounds,
                   ROUND(SUM(CASE WHEN record_type = 'WORK' THEN duration_sec ELSE 0 END) / 60) AS workMinutes,
                   ROUND(SUM(CASE WHEN record_type <> 'WORK' THEN duration_sec ELSE 0 END) / 60) AS breakMinutes,
                   ROUND(SUM(duration_sec) / 60) AS totalMinutes
            FROM pomodoro_record
            WHERE deleted = 0
              AND stat_date BETWEEN #{startDate} AND #{endDate}
            GROUP BY stat_date
            ORDER BY stat_date DESC
            """)
    List<PomodoroDailyStatVO> selectDailyStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
