package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.RagChunk;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 知识库 - 文档分块 Mapper
 */
@Mapper
public interface RagChunkMapper extends BaseMapper<RagChunk> {
}
