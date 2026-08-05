package com.ai.manager.system.service.support.rag;

import com.ai.manager.system.config.RagProperties;
import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 嵌入服务
 *
 * <p>调用 LLM 提供商的 Embedding API 将文本转为向量。
 * 兼容 OpenAI / DeepSeek / 通义千问 的 OpenAI 协议格式。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RagProperties ragProperties;

    // 每批最大文本数：qwen(dashscope text-embedding-v3) 单次请求上限为 10，超批会返回
    // "batch size is invalid, it should not be larger than 10"；OpenAI 上限远高于此，取 10 两者兼容
    private static final int BATCH_SIZE = 10;

    /**
     * 批量生成文本的嵌入向量
     *
     * @param texts  文本列表
     * @param config 提供商配置（含 apiBaseUrl, apiKey, embeddingModel）
     * @return 向量列表，顺序与输入一一对应
     */
    public List<float[]> embed(List<String> texts, AiKnowledgeConfigVO config) {
        // 注意：apiBaseUrl 可能不含 /v1 路径（如 https://api.deepseek.com），
        // Embedding 端点为 {apiBaseUrl}/embeddings。
        // 对于 OpenAI，apiBaseUrl = https://api.openai.com/v1，结果是 https://api.openai.com/v1/embeddings ✅
        // 对于 DeepSeek，apiBaseUrl = https://api.deepseek.com，结果是 https://api.deepseek.com/embeddings ✅
        // 不要在这里添加 /v1，因为 DeepSeek 的 embedding endpoint 不带 /v1 前缀。
        String url = config.getApiBaseUrl().replaceAll("/+$", "") + "/embeddings";
        String model = config.getEmbeddingModel();
        String apiKey = config.getApiKey();

        if (model == null || model.isBlank()) {
            log.warn("嵌入模型未配置，使用空模型名调用，可能被 API 拒绝");
        }

        List<float[]> allEmbeddings = new ArrayList<>();

        // 分批处理
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
            List<float[]> batchResult = callEmbeddingApi(url, model, apiKey, batch);
            allEmbeddings.addAll(batchResult);

            log.debug("嵌入批次 {}/{} 完成：{} 条文本",
                    i / BATCH_SIZE + 1, (texts.size() + BATCH_SIZE - 1) / BATCH_SIZE, batch.size());
        }

        return allEmbeddings;
    }

    /**
     * 为单条查询文本生成嵌入向量（用于检索）
     */
    public float[] embedQuery(String query, AiKnowledgeConfigVO config) {
        List<float[]> results = embed(List.of(query), config);
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    /**
     * 调用 Embedding API
     */
    @SuppressWarnings("unchecked")
    private List<float[]> callEmbeddingApi(String url, String model, String apiKey, List<String> texts) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            EmbeddingRequest requestBody = new EmbeddingRequest();
            requestBody.setModel(model);
            requestBody.setInput(texts);
            // 显式指定输出维度（与 rag_vectors.embedding 列一致）：qwen text-embedding-v3 原生 1024、
            // OpenAI text-embedding-3-small 默认 1536，都通过 dimensions 参数对齐
            requestBody.setDimensions(ragProperties.getEmbeddingDimensions());

            String json = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(json, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || !body.containsKey("data")) {
                log.error("Embedding API 返回异常: {}", body);
                throw new RuntimeException("Embedding API 返回格式异常");
            }

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) body.get("data");
            // 按 index 排序
            dataList.sort((a, b) -> Integer.compare(
                    (Integer) a.getOrDefault("index", 0),
                    (Integer) b.getOrDefault("index", 0)));

            List<float[]> embeddings = new ArrayList<>();
            for (Map<String, Object> item : dataList) {
                List<Double> embeddingList = (List<Double>) item.get("embedding");
                float[] vector = new float[embeddingList.size()];
                for (int j = 0; j < embeddingList.size(); j++) {
                    vector[j] = embeddingList.get(j).floatValue();
                }
                embeddings.add(vector);
            }

            return embeddings;

        } catch (JsonProcessingException e) {
            log.error("序列化嵌入请求失败", e);
            throw new RuntimeException("嵌入请求序列化失败", e);
        } catch (Exception e) {
            log.error("调用 Embedding API 失败: url={}, model={}", url, model, e);
            throw new RuntimeException("调用嵌入 API 失败: " + e.getMessage(), e);
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class EmbeddingRequest {
        private String model;
        private List<String> input;
        private Integer dimensions;
    }
}
