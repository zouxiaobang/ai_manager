package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.domain.dto.PomodoroRecordCreateRequest;
import com.ai.manager.system.domain.entity.PomodoroRecord;
import com.ai.manager.system.domain.vo.PomodoroDailyStatVO;
import com.ai.manager.system.domain.vo.PomodoroRecordVO;
import com.ai.manager.system.domain.vo.PomodoroSummaryVO;
import com.ai.manager.system.mapper.PomodoroRecordMapper;
import com.ai.manager.system.service.PixelDogStateService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PomodoroRecordServiceImpl 服务层单元测试
 * Mock 掉 Mapper 与 PixelDogStateService，覆盖创建（含轮次自增/类型校验）、日期范围查询、统计聚合与 VO 映射。
 */
@ExtendWith(MockitoExtension.class)
class PomodoroRecordServiceImplTest {

    @Mock
    private PomodoroRecordMapper pomodoroRecordMapper;

    @Mock
    private PixelDogStateService pixelDogStateService;

    private PomodoroRecordServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        // 无 Spring 容器时 LambdaQueryWrapper 的 lambda 解析需要元数据缓存，需手动初始化相关实体
        initTable(PomodoroRecord.class);
    }

    private static void initTable(Class<?> clazz) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), clazz);
    }

    @BeforeEach
    void setUp() {
        service = new PomodoroRecordServiceImpl(pixelDogStateService);
        ReflectionTestUtils.setField(service, "baseMapper", pomodoroRecordMapper);
    }

    private PomodoroRecordCreateRequest request(String type, Integer durationSec) {
        PomodoroRecordCreateRequest req = new PomodoroRecordCreateRequest();
        req.setRecordType(type);
        req.setDurationSec(durationSec);
        req.setSource("ADMIN");
        return req;
    }

    @Test
    void createRecord_workType_shouldAutoIncrementRound() {
        when(pomodoroRecordMapper.selectCount(any())).thenReturn(2L);

        PomodoroRecordVO vo = service.createRecord(request("work", 1500));

        assertThat(vo.getRecordType()).isEqualTo("WORK");
        assertThat(vo.getRoundIndex()).isEqualTo(3);
        assertThat(vo.getSource()).isEqualTo("ADMIN");        ArgumentCaptor<PomodoroRecord> captor = ArgumentCaptor.forClass(PomodoroRecord.class);
        verify(pomodoroRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getRoundIndex()).isEqualTo(3);
        // WORK 记录应累计小狗经验
        verify(pixelDogStateService).addXp("POMODORO_ROUND", 20);
    }

    @Test
    void createRecord_breakType_shouldZeroRound() {
        PomodoroRecordVO vo = service.createRecord(request("SHORT_BREAK", 300));

        assertThat(vo.getRecordType()).isEqualTo("SHORT_BREAK");
        assertThat(vo.getRoundIndex()).isZero();
    }

    @Test
    void createRecord_explicitRound_shouldKeepIt() {
        PomodoroRecordCreateRequest req = request("WORK", 1500);
        req.setRoundIndex(4);

        PomodoroRecordVO vo = service.createRecord(req);

        assertThat(vo.getRoundIndex()).isEqualTo(4);
    }

    @Test
    void createRecord_withInvalidType_shouldThrow() {
        assertThatThrownBy(() -> service.createRecord(request("UNKNOWN", 1500)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无效的记录类型");
    }

    @Test
    void createRecord_withInvalidDuration_shouldThrow() {
        assertThatThrownBy(() -> service.createRecord(request("WORK", 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("时长必须大于 0");
    }

    @Test
    void listByDateRange_shouldMapToVO() {
        PomodoroRecord r = new PomodoroRecord();
        r.setId(1L);
        r.setRecordType("WORK");
        r.setDurationSec(1500);
        r.setRoundIndex(1);
        r.setStatDate(LocalDate.of(2026, 8, 2));
        r.setSource("ADMIN");
        r.setDeleted(0);
        when(pomodoroRecordMapper.selectList(any())).thenReturn(List.of(r));

        List<PomodoroRecordVO> result = service.listByDateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecordType()).isEqualTo("WORK");
        assertThat(result.get(0).getClass().getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("deleted");
    }

    @Test
    void listByDateRange_withInvertedRange_shouldThrow() {
        assertThatThrownBy(() -> service.listByDateRange(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("开始日期不能晚于结束日期");
    }

    @Test
    void countTodayWorkRounds_shouldReturnCount() {
        when(pomodoroRecordMapper.selectCount(any())).thenReturn(3L);

        assertThat(service.countTodayWorkRounds()).isEqualTo(3);
    }

    @Test
    void summary_shouldAggregateDailyStats() {
        PomodoroDailyStatVO day1 = new PomodoroDailyStatVO();
        day1.setWorkRounds(2);
        day1.setWorkMinutes(50);
        day1.setBreakMinutes(10);
        PomodoroDailyStatVO day2 = new PomodoroDailyStatVO();
        day2.setWorkRounds(3);
        day2.setWorkMinutes(75);
        day2.setBreakMinutes(20);
        when(pomodoroRecordMapper.selectDailyStats(any(), any())).thenReturn(List.of(day1, day2));

        PomodoroSummaryVO summary = service.summary(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));

        assertThat(summary.getTotalWorkRounds()).isEqualTo(5);
        assertThat(summary.getTotalWorkMinutes()).isEqualTo(125);
        assertThat(summary.getTotalBreakMinutes()).isEqualTo(30);
        assertThat(summary.getActiveDays()).isEqualTo(2);
        assertThat(summary.getAvgWorkMinutesPerDay()).isEqualTo(62.5);
    }
}
