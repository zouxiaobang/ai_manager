package com.ai.manager.system.iot.asr;

/**
 * ASR 会话上下文（请求级参数，与一次识别调用一一对应）。
 *
 * @param sessionId  设备会话 id（贯穿一次语音对话）
 * @param language   识别语言（ISO-639，如 zh / zh-CN，可空则由服务端自动判断）
 * @param sampleRate 音频采样率（Hz），与设备 AudioParams 对齐
 */
public record AsrContext(String sessionId, String language, int sampleRate) {
}
