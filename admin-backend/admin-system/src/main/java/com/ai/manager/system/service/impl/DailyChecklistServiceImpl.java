package com.ai.manager.system.service.impl;

import com.ai.manager.system.domain.dto.DailyChecklistSaveRequest;
import com.ai.manager.system.domain.entity.DailyChecklist;
import com.ai.manager.system.domain.vo.DailyChecklistStatsVO;
import com.ai.manager.system.domain.vo.DailyChecklistVO;
import com.ai.manager.system.mapper.DailyChecklistMapper;
import com.ai.manager.system.service.DailyChecklistService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyChecklistServiceImpl implements DailyChecklistService {

    private final DailyChecklistMapper dailyChecklistMapper;

    @Override
    public List<DailyChecklistVO> getByDate(LocalDate date) {
        List<DailyChecklist> list = dailyChecklistMapper.selectList(
                new LambdaQueryWrapper<DailyChecklist>()
                        .eq(DailyChecklist::getChecklistDate, date)
        );
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveByDate(DailyChecklistSaveRequest request) {
        LocalDate date = request.getDate();
        if (date == null || request.getItems() == null) {
            return;
        }

        for (DailyChecklistSaveRequest.DailyChecklistItemRequest item : request.getItems()) {
            DailyChecklist existing = dailyChecklistMapper.selectOne(
                    new LambdaQueryWrapper<DailyChecklist>()
                            .eq(DailyChecklist::getChecklistDate, date)
                            .eq(DailyChecklist::getItemKey, item.getItemKey())
            );

            if (existing != null) {
                existing.setCompleted(item.getCompleted() != null ? item.getCompleted() : 0);
                existing.setContent(item.getContent());
                dailyChecklistMapper.updateById(existing);
            } else {
                DailyChecklist entity = new DailyChecklist();
                entity.setChecklistDate(date);
                entity.setItemKey(item.getItemKey());
                entity.setCompleted(item.getCompleted() != null ? item.getCompleted() : 0);
                entity.setContent(item.getContent());
                dailyChecklistMapper.insert(entity);
            }
        }
    }

    @Override
    public DailyChecklistStatsVO getStats(LocalDate startDate, LocalDate endDate) {
        List<DailyChecklist> allRecords = dailyChecklistMapper.selectList(
                new LambdaQueryWrapper<DailyChecklist>()
                        .ge(DailyChecklist::getChecklistDate, startDate)
                        .le(DailyChecklist::getChecklistDate, endDate)
                        .orderByAsc(DailyChecklist::getChecklistDate)
        );

        Map<LocalDate, List<DailyChecklist>> groupedByDate = allRecords.stream()
                .collect(Collectors.groupingBy(DailyChecklist::getChecklistDate));

        List<DailyChecklistStatsVO.DailyStatsItem> items = new ArrayList<>();
        int perfectDays = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<DailyChecklist> dayRecords = groupedByDate.getOrDefault(date, Collections.emptyList());
            if (dayRecords.isEmpty()) continue;

            int total = dayRecords.size();
            int completed = (int) dayRecords.stream().filter(r -> r.getCompleted() == 1).count();
            double rate = total > 0 ? (double) completed / total : 0;

            Map<String, List<DailyChecklist>> byPhase = groupByPhaseKey(dayRecords);
            List<DailyChecklistStatsVO.PhaseStatsItem> phaseStats = new ArrayList<>();
            for (Map.Entry<String, List<DailyChecklist>> entry : byPhase.entrySet()) {
                int pt = entry.getValue().size();
                int pc = (int) entry.getValue().stream().filter(r -> r.getCompleted() == 1).count();
                DailyChecklistStatsVO.PhaseStatsItem psi = new DailyChecklistStatsVO.PhaseStatsItem();
                psi.setPhaseKey(entry.getKey());
                psi.setTotalCount(pt);
                psi.setCompletedCount(pc);
                psi.setCompletionRate(pt > 0 ? (double) pc / pt : 0);
                phaseStats.add(psi);
            }

            DailyChecklistStatsVO.DailyStatsItem item = new DailyChecklistStatsVO.DailyStatsItem();
            item.setDate(date.toString());
            item.setTotalCount(total);
            item.setCompletedCount(completed);
            item.setCompletionRate(rate);
            item.setPhaseStats(phaseStats);
            items.add(item);

            if (completed == total && total > 0) {
                perfectDays++;
            }
        }

        DailyChecklistStatsVO.DailyStatsSummary summary = new DailyChecklistStatsVO.DailyStatsSummary();
        summary.setTotalDays(items.size());
        summary.setOverallRate(items.stream().mapToDouble(DailyChecklistStatsVO.DailyStatsItem::getCompletionRate).average().orElse(0));
        summary.setPerfectDays(perfectDays);
        summary.setCurrentStreak(calculateCurrentStreak(items));
        summary.setBestStreak(calculateBestStreak(items));

        DailyChecklistStatsVO vo = new DailyChecklistStatsVO();
        vo.setItems(items);
        vo.setSummary(summary);
        return vo;
    }

    private Map<String, List<DailyChecklist>> groupByPhaseKey(List<DailyChecklist> records) {
        Map<String, List<DailyChecklist>> grouped = new LinkedHashMap<>();
        for (DailyChecklist r : records) {
            String phaseKey = derivePhaseKey(r.getItemKey());
            grouped.computeIfAbsent(phaseKey, k -> new ArrayList<>()).add(r);
        }
        return grouped;
    }

    private String derivePhaseKey(String itemKey) {
        if (itemKey.startsWith("prep_")) return "EVENING_PREP";
        if (itemKey.startsWith("morning_")) return "MORNING";
        if (itemKey.startsWith("focus_")) return "MORNING_FOCUS";
        if (itemKey.startsWith("midday_")) return "MIDDAY_RESET";
        if (itemKey.startsWith("afternoon_")) return "AFTERNOON";
        if (itemKey.startsWith("review_")) return "EVENING_REVIEW";
        return "UNKNOWN";
    }

    private int calculateCurrentStreak(List<DailyChecklistStatsVO.DailyStatsItem> items) {
        int streak = 0;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).getCompletionRate() >= 0.5) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private int calculateBestStreak(List<DailyChecklistStatsVO.DailyStatsItem> items) {
        int best = 0, current = 0;
        for (DailyChecklistStatsVO.DailyStatsItem item : items) {
            if (item.getCompletionRate() >= 0.5) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 0;
            }
        }
        return best;
    }

    private DailyChecklistVO toVO(DailyChecklist entity) {
        DailyChecklistVO vo = new DailyChecklistVO();
        vo.setItemKey(entity.getItemKey());
        vo.setCompleted(entity.getCompleted());
        vo.setContent(entity.getContent());
        return vo;
    }
}
