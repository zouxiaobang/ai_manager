package com.ai.manager.system.controller;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.framework.web.GlobalExceptionHandler;
import com.ai.manager.system.domain.entity.SysUser;
import com.ai.manager.system.service.SysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SysUserController 接口层单元测试
 * 用 standaloneSetup 避免加载完整 Spring 上下文（主配置类在 admin-server 模块），
 * 显式注册 Bean Validation 校验器使 @Valid 生效，Mock Service 隔离依赖。
 */
class SysUserControllerTest {

    private MockMvc mockMvc;
    private SysUserService sysUserService;

    @BeforeEach
    void setUp() {
        sysUserService = mock(SysUserService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SysUserController(sysUserService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void list_shouldReturnPagedUsers() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setNickname("管理员");
        PageResult<SysUser> page = PageUtils.of(List.of(user), 1, 1, 20);
        when(sysUserService.pageUsers(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/system/users").param("page", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].nickname").value("管理员"));
    }

    @Test
    void getById_shouldReturnUser() throws Exception {
        SysUser user = new SysUser();
        user.setId(5L);
        user.setNickname("张三");
        when(sysUserService.getById(5L)).thenReturn(user);

        mockMvc.perform(get("/api/system/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.nickname").value("张三"));
    }

    @Test
    void update_withValidBody_shouldReturnOk() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setNickname("旧名");
        when(sysUserService.getById(1L)).thenReturn(user);
        when(sysUserService.updateById(any(SysUser.class))).thenReturn(true);

        mockMvc.perform(put("/api/system/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新名\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void update_withOverLongNickname_shouldReturn400() throws Exception {
        String longNickname = "n".repeat(51);

        mockMvc.perform(put("/api/system/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + longNickname + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("昵称长度不能超过 50 字"));
    }

    @Test
    void update_withMissingUser_shouldReturn404() throws Exception {
        when(sysUserService.getById(99L)).thenReturn(null);

        mockMvc.perform(put("/api/system/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"任意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
