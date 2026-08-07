package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.AiKnowledgeChatRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeConfigSaveRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeRagBatchImportRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeRagSearchRequest;
import com.ai.manager.system.domain.vo.AiKnowledgeChatResponse;
import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.ai.manager.system.domain.vo.AiKnowledgeProviderInfoVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagBatchImportResultVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagDocumentVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagSearchResultVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagUploadResultVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ai.manager.system.domain.vo.AiKnowledgeRagStatsVO;
import com.ai.manager.system.domain.vo.AiChatCategoryVO;
import com.ai.manager.system.domain.vo.AiChatConversationVO;
import com.ai.manager.system.domain.vo.AiChatSearchResultVO;
import com.ai.manager.system.domain.vo.AiChatUsageVO;
import com.ai.manager.system.domain.dto.AiChatUsageRecordRequest;
import com.ai.manager.system.domain.dto.AiChatCategorySaveRequest;
import com.ai.manager.system.domain.dto.AiChatConversationSaveRequest;
import com.ai.manager.system.service.AiKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 知识库控制器
 *
 * <p>所属模块：AI 知识库模块</p>
 * <p>API路径前缀：/api/ai-knowledge</p>
 * <p>功能描述：提供智能问答、RAG 知识库管理、模型配置管理接口</p>
 */
@RestController
@RequestMapping("/api/ai-knowledge")
@RequiredArgsConstructor
public class AiKnowledgeController {

    private final AiKnowledgeService aiKnowledgeService;

    // ==================== 模型配置 ====================

    /**
     * 获取 AI 模型配置
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ai-knowledge/config</p>
     */
    @GetMapping("/config")
    public ApiResult<AiKnowledgeConfigVO> getConfig() {
        return ApiResult.ok(aiKnowledgeService.getConfig());
    }

    /**
     * 保存 AI 模型配置
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ai-knowledge/config</p>
     */
    @PutMapping("/config")
    public ApiResult<AiKnowledgeConfigVO> saveConfig(@jakarta.validation.Valid @RequestBody AiKnowledgeConfigSaveRequest request) {
        return ApiResult.ok(aiKnowledgeService.saveConfig(request));
    }

    /**
     * 获取所有已配置的提供商信息（用于模型选择器）
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ai-knowledge/providers</p>
     */
    @GetMapping("/providers")
    public ApiResult<List<AiKnowledgeProviderInfoVO>> getProviders() {
        return ApiResult.ok(aiKnowledgeService.getProviders());
    }

    // ==================== 智能问答 ====================

    /**
     * 发送聊天消息
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ai-knowledge/chat</p>
     */
    @PostMapping("/chat")
    public ApiResult<AiKnowledgeChatResponse> chat(@jakarta.validation.Valid @RequestBody AiKnowledgeChatRequest request) {
        return ApiResult.ok(aiKnowledgeService.chat(request));
    }

    /**
     * 流式聊天（SSE）
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ai-knowledge/chat/stream</p>
     */
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@jakarta.validation.Valid @RequestBody AiKnowledgeChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        aiKnowledgeService.chatStream(request, emitter);
        return emitter;
    }

    // ==================== RAG 知识库 ====================

    /**
     * 获取 Embedding 模型配置（独立于 Chat 配置）
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ai-knowledge/rag/embedding-config</p>
     * <p>参数：provider（可选）——指定提供商时返回该提供商已存配置，否则返回当前活动提供商配置</p>
     */
    @GetMapping("/rag/embedding-config")
    public ApiResult<AiKnowledgeConfigVO> getEmbeddingConfig(
            @RequestParam(value = "provider", required = false) String provider) {
        return ApiResult.ok(aiKnowledgeService.getEmbeddingConfig(provider));
    }

    /**
     * 保存 Embedding 模型配置
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ai-knowledge/rag/embedding-config</p>
     */
    @PutMapping("/rag/embedding-config")
    public ApiResult<Void> saveEmbeddingConfig(@jakarta.validation.Valid @RequestBody AiKnowledgeConfigSaveRequest request) {
        aiKnowledgeService.saveEmbeddingConfig(request);
        return ApiResult.ok();
    }

    /**
     * 获取知识库统计
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ai-knowledge/rag/stats</p>
     */
    @GetMapping("/rag/stats")
    public ApiResult<AiKnowledgeRagStatsVO> getRagStats() {
        return ApiResult.ok(aiKnowledgeService.getRagStats());
    }

    /**
     * 获取知识库文档列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ai-knowledge/rag/documents</p>
     */
    @GetMapping("/rag/documents")
    public ApiResult<List<AiKnowledgeRagDocumentVO>> listRagDocuments() {
        return ApiResult.ok(aiKnowledgeService.listRagDocuments());
    }

    /**
     * 重试处理文档
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ai-knowledge/rag/documents/{id}/retry</p>
     */
    @PostMapping("/rag/documents/{id}/retry")
    public ApiResult<Void> retryRagDocument(@PathVariable Long id) {
        aiKnowledgeService.retryRagDocument(id);
        return ApiResult.ok();
    }

    /**
     * 从知识库移除文档
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ai-knowledge/rag/documents/{id}</p>
     */
    @DeleteMapping("/rag/documents/{id}")
    public ApiResult<Void> removeRagDocument(@PathVariable Long id) {
        aiKnowledgeService.removeRagDocument(id);
        return ApiResult.ok();
    }

    /**
     * 搜索知识库
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ai-knowledge/rag/search</p>
     */
    @PostMapping("/rag/search")
    public ApiResult<AiKnowledgeRagSearchResultVO> searchRag(@jakarta.validation.Valid @RequestBody AiKnowledgeRagSearchRequest request) {
        return ApiResult.ok(aiKnowledgeService.searchRag(request));
    }

    /**
     * 上传文档到知识库
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ai-knowledge/rag/upload</p>
     */
    @PostMapping("/rag/upload")
    public ApiResult<AiKnowledgeRagUploadResultVO> uploadRagDocument(@RequestParam("file") MultipartFile file) {
        AiKnowledgeRagUploadResultVO result = aiKnowledgeService.uploadRagDocument(file);
        return ApiResult.ok(result);
    }

    /**
     * 按笔记 ID 导入笔记正文到知识库
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ai-knowledge/rag/import-note/{noteId}</p>
     */
    @PostMapping("/rag/import-note/{noteId}")
    public ApiResult<AiKnowledgeRagUploadResultVO> importNoteToRag(@PathVariable Long noteId) {
        return ApiResult.ok(aiKnowledgeService.importNoteToRag(noteId));
    }

    /**
     * 批量导入笔记到知识库（单篇失败不中断整体，失败明细在返回体中逐条给出）
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ai-knowledge/rag/import-notes</p>
     * <p>body：{@code { "noteIds": [1, 2, 3] }}</p>
     */
    @PostMapping("/rag/import-notes")
    public ApiResult<AiKnowledgeRagBatchImportResultVO> importNotesToRag(
            @jakarta.validation.Valid @RequestBody AiKnowledgeRagBatchImportRequest request) {
        return ApiResult.ok(aiKnowledgeService.importNotesToRag(request.getNoteIds()));
    }

    /**
     * 重建索引
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ai-knowledge/rag/rebuild</p>
     */
    @PostMapping("/rag/rebuild")
    public ApiResult<Void> rebuildRagIndex() {
        aiKnowledgeService.rebuildRagIndex();
        return ApiResult.ok();
    }

    // ==================== 对话管理 ====================

    /**
     * 获取所有分类及对话
     */
    @GetMapping("/chat/categories")
    public ApiResult<java.util.List<AiChatCategoryVO>> getChatCategories() {
        return ApiResult.ok(aiKnowledgeService.getChatCategories());
    }

    /**
     * 创建分类
     */
    @PostMapping("/chat/categories")
    public ApiResult<AiChatCategoryVO> createChatCategory(@RequestBody @jakarta.validation.Valid AiChatCategorySaveRequest request) {
        return ApiResult.ok(aiKnowledgeService.createChatCategory(request));
    }

    /**
     * 重命名分类
     */
    @PutMapping("/chat/categories/{id}")
    public ApiResult<Void> renameChatCategory(@PathVariable Long id, @RequestBody @jakarta.validation.Valid AiChatCategorySaveRequest request) {
        aiKnowledgeService.renameChatCategory(id, request);
        return ApiResult.ok();
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/chat/categories/{id}")
    public ApiResult<Void> deleteChatCategory(@PathVariable Long id) {
        aiKnowledgeService.deleteChatCategory(id);
        return ApiResult.ok();
    }

    /**
     * 在分类下创建对话
     */
    @PostMapping("/chat/categories/{id}/conversations")
    public ApiResult<AiChatConversationVO> createChatConversation(@PathVariable Long id) {
        return ApiResult.ok(aiKnowledgeService.createChatConversation(id));
    }

    /**
     * 更新对话
     */
    @PutMapping("/chat/conversations/{id}")
    public ApiResult<Void> updateChatConversation(@PathVariable Long id, @jakarta.validation.Valid @RequestBody AiChatConversationSaveRequest request) {
        aiKnowledgeService.updateChatConversation(id, request);
        return ApiResult.ok();
    }

    /**
     * 删除对话
     */
    @DeleteMapping("/chat/conversations/{id}")
    public ApiResult<Void> deleteChatConversation(@PathVariable Long id) {
        aiKnowledgeService.deleteChatConversation(id);
        return ApiResult.ok();
    }

    /**
     * 全局搜索对话（按分类名、对话标题、消息内容模糊查询）
     */
    @GetMapping("/chat/search")
    public ApiResult<List<AiChatSearchResultVO>> searchChatConversations(@RequestParam String keyword) {
        return ApiResult.ok(aiKnowledgeService.searchChatConversations(keyword));
    }

    /**
     * 获取聊天用量统计
     */
    @GetMapping("/chat/usage")
    public ApiResult<AiChatUsageVO> getChatUsage() {
        return ApiResult.ok(aiKnowledgeService.getChatUsage());
    }

    /**
     * 记录一次聊天用量
     */
    @PostMapping("/chat/usage")
    public ApiResult<Void> recordChatUsage(@jakarta.validation.Valid @RequestBody AiChatUsageRecordRequest request) {
        aiKnowledgeService.recordChatUsage(request);
        return ApiResult.ok();
    }
}
