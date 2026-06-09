package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.PomodoroRecordCreateRequest;
import com.ai.manager.system.domain.entity.PomodoroRecord;
import com.ai.manager.system.domain.vo.PomodoroDailyStatVO;
import com.ai.manager.system.domain.vo.PomodoroSummaryVO;
import com.ai.manager.system.mapper.PomodoroRecordMapper;
import com.ai.manager.system.service.PomodoroRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class PomodoroRecordServiceImpl extends ServiceImpl<PomodoroRecordMapper, PomodoroRecord>
        implements PomodoroRecordService {

    private static final Set<String> RECORD_TYPES = Set.of("WORK", "SHORT_BREAK", "LONG_BREAK");

    @Override
    public PomodoroRecord createRecord(PomodoroRecordCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getRecordType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "记录类型不能为空");
        }
        String type = request.getRecordType().trim().toUpperCase();
        if (!RECORD_TYPES.contains(type)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "无效的记录类型");
        }
        if (request.getDurationSec() == null || request.getDurationSec() < 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "时长必须大于 0");
        }

        PomodoroRecord record = new PomodoroRecord();
        record.setPlanId(request.getPlanId());
        record.setRecordType(type);
        record.setDurationSec(request.getDurationSec());
        record.setStatDate(LocalDate.now());
        String source = request.getSource();
        record.setSource(StringUtils.hasText(source) ? source.trim().toUpperCase() : "ADMIN");
        record.setRemark(request.getRemark());

        if ("WORK".equals(type)) {
            int round = request.getRoundIndex() != null && request.getRoundIndex() > 0
                    ? request.getRoundIndex()
                    : countTodayWorkRounds() + 1;
            record.setRoundIndex(round);
        } else {
            record.setRoundIndex(0);
        }

        save(record);
        return record;
    }

    @Override
    public List<PomodoroRecord> listByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = normalizeRange(startDate, endDate);
        return list(new LambdaQueryWrapper<PomodoroRecord>()
                .between(PomodoroRecord::getStatDate, range[0], range[1])
                .orderByDesc(PomodoroRecord::getCreateTime));
    }

    @Override
    public List<PomodoroDailyStatVO> dailyStats(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = normalizeRange(startDate, endDate);
        return baseMapper.selectDailyStats(range[0], range[1]);
    }

    @Override
    public PomodoroSummaryVO summary(LocalDate startDate, LocalDate endDate) {
        List<PomodoroDailyStatVO> daily = dailyStats(startDate, endDate);
        PomodoroSummaryVO vo = new PomodoroSummaryVO();
        int workRounds = 0;
        int workMinutes = 0;
        int breakMinutes = 0;
        for (PomodoroDailyStatVO day : daily) {
            workRounds += day.getWorkRounds() == null ? 0 : day.getWorkRounds();
            workMinutes += day.getWorkMinutes() == null ? 0 : day.getWorkMinutes();
            breakMinutes += day.getBreakMinutes() == null ? 0 : day.getBreakMinutes();
        }
        vo.setTotalWorkRounds(workRounds);
        vo.setTotalWorkMinutes(workMinutes);
        vo.setTotalBreakMinutes(breakMinutes);
        vo.setActiveDays(daily.size());
        vo.setAvgWorkMinutesPerDay(daily.isEmpty() ? 0.0 : (double) workMinutes / daily.size());
        return vo;
    }

    @Override
    public int countTodayWorkRounds() {
        Long count = count(new LambdaQueryWrapper<PomodoroRecord>()
                .eq(PomodoroRecord::getStatDate, LocalDate.now())
                .eq(PomodoroRecord::getRecordType, "WORK"));
        return count == null ? 0 : count.intValue();
    }

    private LocalDate[] normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(6) : startDate;
        if (start.isAfter(end)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "开始日期不能晚于结束日期");
        }
        return new LocalDate[]{start, end};
    }
}
