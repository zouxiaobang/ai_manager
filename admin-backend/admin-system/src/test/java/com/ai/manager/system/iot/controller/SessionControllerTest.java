package com.ai.manager.system.iot.controller;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.framework.web.GlobalExceptionHandler;
import com.ai.manager.system.iot.domain.vo.OnlineSessionVO;
import com.ai.manager.system.iot.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SessionController 接口层单元测试（standaloneSetup）。
 */
class SessionControllerTest {

    private MockMvc mockMvc;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = mock(SessionService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SessionController(sessionService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void page_shouldReturnPagedSessions() throws Exception {
        OnlineSessionVO vo = new OnlineSessionVO();
        vo.setId(1L);
        vo.setSessionId("s1");
        vo.setOnline(true);
        when(sessionService.pageSessions(1L, 20L, null))
                .thenReturn(PageUtils.of(List.of(vo), 1, 1, 20));

        mockMvc.perform(get("/api/iot/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].sessionId").value("s1"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void page_withOnlineFilter_shouldForward() throws Exception {
        when(sessionService.pageSessions(1L, 20L, true))
                .thenReturn(PageResult.empty(1, 20));

        mockMvc.perform(get("/api/iot/session").param("online", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
