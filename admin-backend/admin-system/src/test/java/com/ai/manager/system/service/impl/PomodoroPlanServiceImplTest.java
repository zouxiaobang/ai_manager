package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.PomodoroPlanSaveRequest;
import com.ai.manager.system.domain.entity.PomodoroPlan;
import com.ai.manager.system.domain.vo.PomodoroPlanVO;
import com.ai.manager.system.mapper.PomodoroPlanMapper;
import com.ai.manager.system.service.PomodoroSessionService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PomodoroPlanServiceImpl 服务层单元测试
 * Mock 掉 Mapper 与 PomodoroSessionService，覆盖分页/启用列表/默认计划/增删改与 VO 映射（剔除 deleted 字段）。
 */
@ExtendWith(MockitoExtension.class)
class PomodoroPlanServiceImplTest {

    @Mock
    private PomodoroPlanMapper pomodoroPlanMapper;

    @Mock
    private PomodoroSessionService pomodoroSessionService;

    private PomodoroPlanServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        // 无 Spring 容器时 LambdaQueryWrapper 的 lambda 解析需要元数据缓存，需手动初始化相关实体
        initTable(PomodoroPlan.class);
    }

    private static void initTable(Class<?> clazz) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), clazz);
    }

    @BeforeEach
    void setUp() {
        service = new PomodoroPlanServiceImpl(pomodoroSessionService);
        ReflectionTestUtils.setField(service, "baseMapper", pomodoroPlanMapper);
    }

    private PomodoroPlan entity(Long id, String title, int isDefault) {
        PomodoroPlan p = new PomodoroPlan();
        p.setId(id);
        p.setTitle(title);
        p.setWorkDurationMin(25);
        p.setShortBreakMin(5);
        p.setLongBreakMin(15);
        p.setRoundsBeforeLongBreak(4);
        p.setDailyGoalRounds(8);
        p.setDailyGoalMinutes(200);
        p.setIsDefault(isDefault);
        p.setStatus("ENABLED");
        p.setDeleted(0);
        return p;
    }

    private PomodoroPlanSaveRequest request(String title) {
        PomodoroPlanSaveRequest req = new PomodoroPlanSaveRequest();
        req.setTitle(title);
        req.setWorkDurationMin(25);
        return req;
    }

    @Test
    void pagePlans_shouldMapToVOWithoutDeleted() {
        Page<PomodoroPlan> entityPage = new Page<>(1, 10);
        entityPage.setRecords(List.of(entity(1L, "计划A", 1)));
        entityPage.setTotal(1);
        when(pomodoroPlanMapper.selectPage(any(), any())).thenReturn(entityPage);

        PageResult<PomodoroPlanVO> result = service.pagePlans(1L, 10L);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getTitle()).isEqualTo("计划A");
        assertThat(result.getRecords().get(0).getClass().getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("deleted");
    }

    @Test
    void listEnabled_shouldReturnOnlyVO() {
        when(pomodoroPlanMapper.selectList(any())).thenReturn(List.of(entity(1L, "计划A", 1)));

        List<PomodoroPlanVO> result = service.listEnabled();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("计划A");
        assertThat(result.get(0).getClass().getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("deleted");
    }

    @Test
    void getDefaultPlan_whenDefaultExists_shouldReturnIt() {
        when(pomodoroPlanMapper.selectOne(any(), anyBoolean())).thenReturn(entity(1L, "默认计划", 1));

        PomodoroPlanVO vo = service.getDefaultPlan();

        assertThat(vo.getTitle()).isEqualTo("默认计划");
    }

    @Test
    void getDefaultPlan_whenNoDefault_shouldFallbackToFirstEnabled() {
        when(pomodoroPlanMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(pomodoroPlanMapper.selectList(any())).thenReturn(List.of(entity(2L, "回退计划", 0)));

        PomodoroPlanVO vo = service.getDefaultPlan();

        assertThat(vo.getTitle()).isEqualTo("回退计划");
    }

    @Test
    void getDefaultPlan_whenNone_shouldThrow() {
        when(pomodoroPlanMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(pomodoroPlanMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getDefaultPlan())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂无可用番茄钟计划");
    }

    @Test
    void getPlan_whenMissing_shouldReturnNull() {
        when(pomodoroPlanMapper.selectById(99L)).thenReturn(null);

        assertThat(service.getPlan(99L)).isNull();
    }

    @Test
    void createPlan_asDefault_shouldClearOtherDefaults() {
        when(pomodoroSessionService.isPlanEditBlocked()).thenReturn(false);
        PomodoroPlanSaveRequest req = request("新计划");
        req.setAsDefault(true);

        PomodoroPlanVO vo = service.createPlan(req);

        assertThat(vo.getTitle()).isEqualTo("新计划");
        assertThat(vo.getIsDefault()).isEqualTo(1);
        verify(pomodoroPlanMapper).insert(any(PomodoroPlan.class));
        verify(pomodoroPlanMapper).update(any(), any());
    }

    @Test
    void createPlan_whenTimingBlocked_shouldThrow() {
        when(pomodoroSessionService.isPlanEditBlocked()).thenReturn(true);

        assertThatThrownBy(() -> service.createPlan(request("新计划")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正在计时");
    }

    @Test
    void updatePlan_shouldUpdateAndReturnVO() {
        when(pomodoroSessionService.isPlanEditBlocked()).thenReturn(false);
        when(pomodoroPlanMapper.selectById(1L)).thenReturn(entity(1L, "旧名", 0));

        PomodoroPlanVO vo = service.updatePlan(1L, request("新名"));

        assertThat(vo.getTitle()).isEqualTo("新名");        verify(pomodoroPlanMapper).updateById(any(PomodoroPlan.class));
    }

    @Test
    void updatePlan_whenMissing_shouldThrow() {
        when(pomodoroSessionService.isPlanEditBlocked()).thenReturn(false);
        when(pomodoroPlanMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updatePlan(99L, request("新计划")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deletePlan_whenDefault_shouldReject() {
        when(pomodoroSessionService.isPlanEditBlocked()).thenReturn(false);
        when(pomodoroPlanMapper.selectById(1L)).thenReturn(entity(1L, "默认", 1));

        assertThatThrownBy(() -> service.deletePlan(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除默认计划");
        verify(pomodoroPlanMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deletePlan_whenNormal_shouldRemove() {
        when(pomodoroSessionService.isPlanEditBlocked()).thenReturn(false);
        when(pomodoroPlanMapper.selectById(2L)).thenReturn(entity(2L, "普通", 0));

        service.deletePlan(2L);

        verify(pomodoroPlanMapper).deleteById(any(Long.class));
    }
}
