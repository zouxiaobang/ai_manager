package com.ai.manager.system.service.support.rag;

import com.ai.manager.system.config.RagProperties;
import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * EmbeddingService 单测
 *
 * <p>核心验证分批逻辑：每批文本数不得超过 10（qwen dashscope text-embedding-v3 单次上限为 10，
 * 超批会返回 "batch size is invalid, it should not be larger than 10"）。</p>
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RagProperties ragProperties = new RagProperties();
    private EmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new EmbeddingService(restTemplate, objectMapper, ragProperties);
    }

    @Test
    void embed_超过10条自动分批且每批不超过10条() throws Exception {
        List<String> texts = IntStream.range(0, 25).mapToObj(i -> "text-" + i).collect(Collectors.toList());
        AiKnowledgeConfigVO cfg = config();

        List<Integer> capturedSizes = new ArrayList<>();
        List<Integer> capturedDimensions = new ArrayList<>();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenAnswer(inv -> {
                    HttpEntity<String> entity = inv.getArgument(1);
                    Map<?, ?> body = objectMapper.readValue(entity.getBody(), Map.class);
                    List<?> input = (List<?>) body.get("input");
                    capturedSizes.add(input.size());
                    capturedDimensions.add((Integer) body.get("dimensions"));
                    return okEmbeddingResponse(input.size());
                });

        List<float[]> result = service.embed(texts, cfg);

        // 25 条 → 10/10/5 三批，均不超 qwen 上限 10
        assertThat(capturedSizes).containsExactly(10, 10, 5);
        // 请求显式带 dimensions（与 rag_vectors.embedding 列一致）
        assertThat(capturedDimensions).containsOnly(1024);
        assertThat(result).hasSize(25);
        assertThat(result.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void embed_少于10条单批提交() throws Exception {
        List<String> texts = List.of("a", "b", "c");
        AiKnowledgeConfigVO cfg = config();

        List<Integer> capturedSizes = new ArrayList<>();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenAnswer(inv -> {
                    HttpEntity<String> entity = inv.getArgument(1);
                    Map<?, ?> body = objectMapper.readValue(entity.getBody(), Map.class);
                    capturedSizes.add(((List<?>) body.get("input")).size());
                    return okEmbeddingResponse(capturedSizes.get(0));
                });

        List<float[]> result = service.embed(texts, cfg);

        assertThat(capturedSizes).containsExactly(3);
        assertThat(result).hasSize(3);
    }

    private AiKnowledgeConfigVO config() {
        AiKnowledgeConfigVO cfg = new AiKnowledgeConfigVO();
        cfg.setApiBaseUrl("https://api.openai.com/v1");
        cfg.setApiKey("sk-test");
        cfg.setEmbeddingModel("text-embedding-3-small");
        return cfg;
    }

    private ResponseEntity<Map> okEmbeddingResponse(int size) {
        Map<String, Object> resp = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        for (int j = 0; j < size; j++) {
            Map<String, Object> item = new HashMap<>();
            item.put("index", j);
            item.put("embedding", List.of(0.1, 0.2, 0.3));
            data.add(item);
        }
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }
}
