package com.ai.manager.system.service.impl;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.entity.SysUser;
import com.ai.manager.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserServiceImpl 服务层单元测试
 * MyBatis-Plus 的 ServiceImpl 通过父类泛型字段 baseMapper 访问 Mapper，
 * Mockito @InjectMocks 对泛型擦除的父类字段注入不可靠，改用 ReflectionTestUtils 显式注入。
 * 验证分页参数归一化与结果组装。
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    private SysUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysUserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", sysUserMapper);
    }

    @Test
    void pageUsers_withValidParams_shouldReturnPagedUsers() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setNickname("管理员");
        Page<SysUser> dbPage = new Page<>(2, 10);
        dbPage.setRecords(List.of(user));
        dbPage.setTotal(1);
        when(sysUserMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(dbPage);

        PageResult<SysUser> result = service.pageUsers(2L, 10L);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getNickname()).isEqualTo("管理员");
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
    }

    @Test
    void pageUsers_withNullParams_shouldUseDefaults() {
        Page<SysUser> dbPage = new Page<>(1, 20);
        dbPage.setRecords(List.of());
        dbPage.setTotal(0);
        when(sysUserMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(dbPage);

        service.pageUsers(null, null);

        ArgumentCaptor<Page<SysUser>> captor = ArgumentCaptor.forClass(Page.class);
        verify(sysUserMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getCurrent()).isEqualTo(1);
        assertThat(captor.getValue().getSize()).isEqualTo(20);
    }

    @Test
    void pageUsers_withOversizedPageSize_shouldCapAtMax() {
        Page<SysUser> dbPage = new Page<>(1, 100);
        dbPage.setRecords(List.of());
        dbPage.setTotal(0);
        when(sysUserMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(dbPage);

        service.pageUsers(0L, 10000L);

        ArgumentCaptor<Page<SysUser>> captor = ArgumentCaptor.forClass(Page.class);
        verify(sysUserMapper).selectPage(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getCurrent()).isEqualTo(1);
        assertThat(captor.getValue().getSize()).isEqualTo(100);
    }
}
