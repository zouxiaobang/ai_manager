package com.ai.manager.system.service.support.llm;

import com.ai.manager.system.domain.dto.AiKnowledgeChatRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 聊天消息构建器。
 * <p>将前端传入的 {@link AiKnowledgeChatRequest} 转换为 Spring AI 的 {@link Message} 列表，
 * 消除 chat / chatStream 方法中的重复构建逻辑。</p>
 */
@Component
public class PromptBuilder {

    /** 默认系统提示词 */
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个有用的AI助手，请用中文回答用户的问题。";

    /**
     * 根据请求构建完整的消息列表
     *
     * @param request 聊天请求（含问题、历史消息）
     * @return Spring AI Message 列表（system prompt + history + current question）
     */
    public List<Message> buildMessages(AiKnowledgeChatRequest request) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(DEFAULT_SYSTEM_PROMPT));

        if (request.getHistory() != null) {
            addHistory(messages, request.getHistory());
        }

        messages.add(new UserMessage(request.getQuestion()));
        return messages;
    }

    /**
     * 构建含 RAG 上下文的聊天消息
     *
     * @param request   聊天请求
     * @param ragContext RAG 检索到的上下文文本
     * @return Spring AI Message 列表（含 RAG 上下文的 system prompt + history + current question）
     */
    public List<Message> buildRagMessages(AiKnowledgeChatRequest request, String ragContext) {
        List<Message> messages = new ArrayList<>();

        // RAG 专用的系统提示（含知识库上下文）
        String systemPrompt;
        if (ragContext != null && !ragContext.isBlank()) {
            systemPrompt = """
                    你是一个有用的AI助手，请用中文回答用户的问题。

                    以下是与用户问题相关的知识库资料，请**严格基于这些资料**回答。
                    如果资料不足以回答问题，请如实告知用户你不知道，不要编造信息。

                    ===== 知识库资料 =====
                    %s
                    ===== 知识库资料结束 =====
                    """.formatted(ragContext);
        } else {
            systemPrompt = DEFAULT_SYSTEM_PROMPT;
        }
        messages.add(new SystemMessage(systemPrompt));

        if (request.getHistory() != null) {
            addHistory(messages, request.getHistory());
        }

        messages.add(new UserMessage(request.getQuestion()));
        return messages;
    }

    /**
     * 添加历史消息到消息列表
     */
    private void addHistory(List<Message> messages, List<AiKnowledgeChatRequest.ChatHistoryItem> history) {
        for (AiKnowledgeChatRequest.ChatHistoryItem item : history) {
            switch (item.getRole()) {
                case "system" -> messages.add(new SystemMessage(item.getContent()));
                case "user" -> messages.add(new UserMessage(item.getContent()));
                case "assistant" ->
                        messages.add(new org.springframework.ai.chat.messages.AssistantMessage(item.getContent()));
                default -> messages.add(new UserMessage(item.getContent()));
            }
        }
    }
}
