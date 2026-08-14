package com.ai.manager.system.controller;

import com.ai.manager.framework.web.GlobalExceptionHandler;
import com.ai.manager.system.domain.dto.AiChatBookmarkSaveRequest;
import com.ai.manager.system.domain.vo.AiChatBookmarkVO;
import com.ai.manager.system.service.AiKnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiKnowledgeController 书签管理接口层单元测试
 *
 * <p>standalone MockMvc + LocalValidatorFactoryBean + GlobalExceptionHandler，
 * mock AiKnowledgeService，覆盖 5 个书签端点及请求体校验失败分支。</p>
 */
class AiKnowledgeControllerTest {

    private MockMvc mockMvc;
    private AiKnowledgeService aiKnowledgeService;

    @BeforeEach
    void setUp() {
        aiKnowledgeService = mock(AiKnowledgeService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AiKnowledgeController(aiKnowledgeService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listChatBookmarks_shouldReturnOk() throws Exception {
        AiChatBookmarkVO vo = new AiChatBookmarkVO();
        vo.setId(1L);
        vo.setName("进度标记");
        when(aiKnowledgeService.listChatBookmarks(10L)).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/ai-knowledge/chat/conversations/10/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("进度标记"));
    }

    @Test
    void createChatBookmark_withValidBody_shouldReturnOk() throws Exception {
        AiChatBookmarkVO vo = new AiChatBookmarkVO();
        vo.setId(1L);
        vo.setName("进度标记");
        when(aiKnowledgeService.createChatBookmark(eq(10L), any(AiChatBookmarkSaveRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/ai-knowledge/chat/conversations/10/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "进度标记", "msgId": "m1", "msgOffsetTop": 120, "scrollTop": 800}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("进度标记"));
    }

    @Test
    void createChatBookmark_missingName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/ai-knowledge/chat/conversations/10/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"msgOffsetTop": 120, "scrollTop": 800}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("标记名称不能为空"));
    }

    @Test
    void renameChatBookmark_withValidBody_shouldReturnOk() throws Exception {
        mockMvc.perform(put("/api/ai-knowledge/chat/bookmarks/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "新名字", "msgOffsetTop": 10, "scrollTop": 20}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(aiKnowledgeService).renameChatBookmark(eq(5L), any(AiChatBookmarkSaveRequest.class));
    }

    @Test
    void deleteChatBookmark_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/ai-knowledge/chat/bookmarks/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(aiKnowledgeService).deleteChatBookmark(5L);
    }

    @Test
    void deleteAllChatBookmarks_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/ai-knowledge/chat/conversations/10/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(aiKnowledgeService).deleteAllChatBookmarks(10L);
    }
}
