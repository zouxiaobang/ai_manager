package com.ai.manager.system.service.impl;

import com.ai.manager.system.domain.dto.DailyChecklistSaveRequest;
import com.ai.manager.system.domain.entity.DailyChecklist;
import com.ai.manager.system.domain.vo.DailyChecklistStatsVO;
import com.ai.manager.system.domain.vo.DailyChecklistVO;
import com.ai.manager.system.mapper.DailyChecklistMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DailyChecklistServiceImpl 服务层单元测试
 * Mock 掉 Mapper，覆盖按日期查询、保存（新增/更新分支）与统计聚合。
 */
@ExtendWith(MockitoExtension.class)
class DailyChecklistServiceImplTest {

    @Mock
    private DailyChecklistMapper dailyChecklistMapper;

    @InjectMocks
    private DailyChecklistServiceImpl service;

    private DailyChecklist entity(String itemKey, int completed) {
        DailyChecklist e = new DailyChecklist();
        e.setChecklistDate(LocalDate.of(2026, 8, 2));
        e.setItemKey(itemKey);
        e.setCompleted(completed);
        e.setContent("内容-" + itemKey);
        return e;
    }

    @Test
    void getByDate_shouldMapEntitiesToVOs() {
        when(dailyChecklistMapper.selectList(any())).thenReturn(List.of(entity("morning_water", 1)));

        List<DailyChecklistVO> result = service.getByDate(LocalDate.of(2026, 8, 2));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemKey()).isEqualTo("morning_water");
        assertThat(result.get(0).getCompleted()).isEqualTo(1);
        assertThat(result.get(0).getContent()).isEqualTo("内容-morning_water");
    }

    @Test
    void saveByDate_withNullDate_shouldDoNothing() {
        DailyChecklistSaveRequest req = new DailyChecklistSaveRequest();
        req.setDate(null);
        req.setItems(List.of());

        service.saveByDate(req);

        verify(dailyChecklistMapper, never()).selectOne(any());
    }

    @Test
    void saveByDate_withNewItem_shouldInsert() {
        DailyChecklistSaveRequest req = new DailyChecklistSaveRequest();
        req.setDate(LocalDate.of(2026, 8, 2));
        DailyChecklistSaveRequest.DailyChecklistItemRequest item = new DailyChecklistSaveRequest.DailyChecklistItemRequest();
        item.setItemKey("morning_water");
        item.setCompleted(1);
        item.setContent("喝水");
        req.setItems(List.of(item));

        when(dailyChecklistMapper.selectOne(any())).thenReturn(null);

        service.saveByDate(req);

        ArgumentCaptor<DailyChecklist> captor = ArgumentCaptor.forClass(DailyChecklist.class);
        verify(dailyChecklistMapper).insert(captor.capture());
        assertThat(captor.getValue().getItemKey()).isEqualTo("morning_water");
        assertThat(captor.getValue().getCompleted()).isEqualTo(1);
    }

    @Test
    void saveByDate_withExistingItem_shouldUpdate() {
        DailyChecklistSaveRequest req = new DailyChecklistSaveRequest();
        req.setDate(LocalDate.of(2026, 8, 2));
        DailyChecklistSaveRequest.DailyChecklistItemRequest item = new DailyChecklistSaveRequest.DailyChecklistItemRequest();
        item.setItemKey("morning_water");
        item.setCompleted(0);
        req.setItems(List.of(item));

        DailyChecklist existing = entity("morning_water", 1);
        when(dailyChecklistMapper.selectOne(any())).thenReturn(existing);

        service.saveByDate(req);

        verify(dailyChecklistMapper, never()).insert(any(DailyChecklist.class));
        verify(dailyChecklistMapper).updateById(existing);
        assertThat(existing.getCompleted()).isZero();
    }

    @Test
    void getStats_shouldAggregateRatesStreaksAndPerfectDays() {
        // 两天：8/1 全部完成，8/2 全部未完成
        DailyChecklist d1 = new DailyChecklist();
        d1.setChecklistDate(LocalDate.of(2026, 8, 1));
        d1.setItemKey("morning_water");
        d1.setCompleted(1);
        DailyChecklist d2 = new DailyChecklist();
        d2.setChecklistDate(LocalDate.of(2026, 8, 1));
        d2.setItemKey("focus_1");
        d2.setCompleted(1);
        DailyChecklist d3 = new DailyChecklist();
        d3.setChecklistDate(LocalDate.of(2026, 8, 2));
        d3.setItemKey("morning_water");
        d3.setCompleted(0);
        when(dailyChecklistMapper.selectList(any()))
                .thenReturn(List.of(d1, d2, d3));

        DailyChecklistStatsVO stats = service.getStats(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));

        assertThat(stats.getItems()).hasSize(2);
        assertThat(stats.getSummary().getTotalDays()).isEqualTo(2);
        assertThat(stats.getSummary().getPerfectDays()).isEqualTo(1);
        // 当前连续达标（>=50%）：8/2 完成率 0 中断，故为 0
        assertThat(stats.getSummary().getCurrentStreak()).isZero();
        assertThat(stats.getSummary().getBestStreak()).isEqualTo(1);
        assertThat(stats.getSummary().getOverallRate())
                .isEqualTo((1.0 + 0.0) / 2);
    }
}
