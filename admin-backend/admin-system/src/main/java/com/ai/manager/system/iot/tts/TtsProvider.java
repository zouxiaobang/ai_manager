package com.ai.manager.system.iot.tts;

/**
 * TTS（语音合成，Text To Speech）提供方抽象。
 * <p>
 * 实现类负责把文字合成为音频字节，可插拔替换厂商；后端按 {@code IotProperties.tts.type}
 * 通过 {@link TtsProviderFactory} 选择具体实现，换厂商只改配置。
 */
public interface TtsProvider {

    /**
     * 语音合成：文字 → 音频字节。
     *
     * @param text 待合成的文字
     * @param ctx  会话上下文（sessionId / voice / sampleRate）
     * @return 合成结果（音频字节 + 格式 + 采样率 + 耗时）
     */
    TtsResult synthesize(String text, TtsContext ctx);
}
