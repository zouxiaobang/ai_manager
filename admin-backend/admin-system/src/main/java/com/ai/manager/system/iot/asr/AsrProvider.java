package com.ai.manager.system.iot.asr;

/**
 * ASR（语音识别，Audio Speech Recognition）提供方抽象。
 * <p>
 * 实现类负责把 WAV 音频转成文字，可插拔替换厂商；后端按 {@code IotProperties.asr.type}
 * 通过 {@link AsrProviderFactory} 选择具体实现，换厂商只改配置。
 */
public interface AsrProvider {

    /**
     * 语音识别：16bit PCM 的 WAV 音频 → 文字。
     *
     * @param wavAudio WAV 音频字节（whisper 等云端接口要求的文件格式）
     * @param ctx      会话上下文（sessionId / language / sampleRate）
     * @return 识别结果（文字 + 原始响应 + 耗时）
     */
    AsrResult transcribe(byte[] wavAudio, AsrContext ctx);
}
