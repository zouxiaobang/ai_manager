package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.AiKnowledgeConfigSaveRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeRagSearchRequest;
import com.ai.manager.system.domain.vo.AiKnowledgeChatResponse;
import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.ai.manager.system.domain.vo.AiKnowledgeProviderInfoVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagDocumentVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagSearchResultVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagStatsVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagUploadResultVO;
import com.ai.manager.system.domain.dto.AiKnowledgeChatRequest;
import com.ai.manager.system.domain.dto.AiChatCategorySaveRequest;
import com.ai.manager.system.domain.dto.AiChatConversationSaveRequest;
import com.ai.manager.system.domain.vo.AiChatCategoryVO;
import com.ai.manager.system.domain.vo.AiChatConversationVO;
import com.ai.manager.system.domain.vo.AiChatSearchResultVO;
import com.ai.manager.system.domain.vo.AiChatUsageVO;
import com.ai.manager.system.domain.dto.AiChatUsageRecordRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 知识库服务接口
 *
 * <p>所属模块：AI 知识库模块</p>
 * <p>功能描述：提供智能问答、RAG 知识库管理、模型配置管理功能</p>
 */
public interface AiKnowledgeService {

    // ==================== 模型配置 ====================

    /**
     * 获取 AI 模型配置
     */
    AiKnowledgeConfigVO getConfig();

    /**
     * 保存 AI 模型配置
     */
    AiKnowledgeConfigVO saveConfig(AiKnowledgeConfigSaveRequest request);

    /**
     * 获取所有已配置的提供商列表
     */
    java.util.List<AiKnowledgeProviderInfoVO> getProviders();

    // ==================== 智能问答 ====================

    /**
     * 发送聊天消息
     */
    AiKnowledgeChatResponse chat(AiKnowledgeChatRequest request);

    /**
     * 流式发送聊天消息（SSE  Server-Sent Events）
     */
    void chatStream(AiKnowledgeChatRequest request, SseEmitter emitter);

    // ==================== RAG 知识库 ====================

    /**
     * 获取 Embedding 模型配置（用于 RAG 向量嵌入，与 chat 配置独立）
     */
    AiKnowledgeConfigVO getEmbeddingConfig();

    /**
     * 保存 Embedding 模型配置
     */
    void saveEmbeddingConfig(AiKnowledgeConfigSaveRequest request);

    /**
     * 获取知识库统计
     */
    AiKnowledgeRagStatsVO getRagStats();

    /**
     * 获取知识库文档列表
     */
    List<AiKnowledgeRagDocumentVO> listRagDocuments();

    /**
     * 上传并处理文档
     */
    AiKnowledgeRagUploadResultVO uploadRagDocument(MultipartFile file);

    /**
     * 重试处理文档
     */
    void retryRagDocument(Long id);

    /**
     * 从知识库移除文档（含向量）
     */
    void removeRagDocument(Long id);

    /**
     * 搜索知识库
     */
    AiKnowledgeRagSearchResultVO searchRag(AiKnowledgeRagSearchRequest request);

    /**
     * 重建索引
     */
    void rebuildRagIndex();

    // ==================== 对话管理 ====================

    /**
     * 获取所有分类及对话
     */
    List<AiChatCategoryVO> getChatCategories();

    /**
     * 创建分类
     */
    AiChatCategoryVO createChatCategory(AiChatCategorySaveRequest request);

    /**
     * 重命名分类
     */
    void renameChatCategory(Long id, AiChatCategorySaveRequest request);

    /**
     * 删除分类（级联删除对话）
     */
    void deleteChatCategory(Long id);

    /**
     * 在分类下创建对话
     */
    AiChatConversationVO createChatConversation(Long categoryId);

    /**
     * 更新对话（标题/消息）
     */
    void updateChatConversation(Long id, AiChatConversationSaveRequest request);

    /**
     * 删除对话
     */
    void deleteChatConversation(Long id);

    /**
     * 全局搜索对话（按分类名、对话标题、消息内容）
     */
    List<AiChatSearchResultVO> searchChatConversations(String keyword);

    /**
     * 获取聊天用量统计
     */
    AiChatUsageVO getChatUsage();

    /**
     * 记录一次聊天用量
     */
    void recordChatUsage(AiChatUsageRecordRequest request);
}
