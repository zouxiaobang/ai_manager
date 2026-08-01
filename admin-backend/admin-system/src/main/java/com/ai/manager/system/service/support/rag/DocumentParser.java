package com.ai.manager.system.service.support.rag;

import java.io.InputStream;

/**
 * 文档解析器接口
 *
 * <p>将不同格式的文档解析为纯文本，供后续分块和嵌入使用。</p>
 */
public interface DocumentParser {

    /**
     * 解析文档为纯文本
     *
     * @param inputStream 文件输入流
     * @param fileType    文件类型 (pdf / txt / md / html / docx)
     * @return 提取的纯文本内容
     */
    String parse(InputStream inputStream, String fileType);
}
