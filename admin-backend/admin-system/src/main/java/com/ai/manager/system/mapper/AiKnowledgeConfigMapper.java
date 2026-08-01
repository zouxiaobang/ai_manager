package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.AiKnowledgeConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 知识库 - 配置 Mapper
 */
@Mapper
public interface AiKnowledgeConfigMapper extends BaseMapper<AiKnowledgeConfig> {
}
