package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.RagDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 知识库 - 文档 Mapper
 */
@Mapper
public interface RagDocumentMapper extends BaseMapper<RagDocument> {
}
