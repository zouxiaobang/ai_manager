package com.ai.manager.system.controller;

import com.ai.manager.framework.web.GlobalExceptionHandler;
import com.ai.manager.system.domain.dto.DailyChecklistSaveRequest;
import com.ai.manager.system.domain.vo.DailyChecklistVO;
import com.ai.manager.system.service.DailyChecklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DailyChecklistController 接口层单元测试
 * 覆盖按日期查询、统计，以及保存时的嵌套列表参数校验。
 */
class DailyChecklistControllerTest {

    private MockMvc mockMvc;
    private DailyChecklistService dailyChecklistService;

    @BeforeEach
    void setUp() {
        dailyChecklistService = mock(DailyChecklistService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new DailyChecklistController(dailyChecklistService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getByDate_shouldReturnRecords() throws Exception {
        DailyChecklistVO vo = new DailyChecklistVO();
        vo.setItemKey("morning_water");
        vo.setCompleted(1);
        when(dailyChecklistService.getByDate(any())).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/24hour").param("date", "2026-08-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].itemKey").value("morning_water"));
    }

    @Test
    void save_withValidBody_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/24hour")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-08-02",
                                  "items": [
                                    { "itemKey": "morning_water", "completed": 1, "content": "喝水" }
                                  ]
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(dailyChecklistService).saveByDate(any(DailyChecklistSaveRequest.class));
    }

    @Test
    void save_withMissingDate_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/24hour")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "itemKey": "morning_water", "completed": 1 }
                                  ]
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("日期不能为空"));
    }

    @Test
    void save_withEmptyItems_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/24hour")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-08-02\",\"items\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void save_withBlankItemKey_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/24hour")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-08-02",
                                  "items": [
                                    { "itemKey": "", "completed": 1 }
                                  ]
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("条目键不能为空"));
    }

    @Test
    void getStats_shouldReturnStats() throws Exception {
        when(dailyChecklistService.getStats(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/24hour/stats")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
