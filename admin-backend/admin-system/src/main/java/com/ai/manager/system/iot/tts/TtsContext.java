package com.ai.manager.system.iot.tts;

/**
 * TTS 会话上下文（请求级参数，与一次合成调用一一对应）。
 *
 * @param sessionId  设备会话 id（贯穿一次语音对话）
 * @param voice      音色（为空时回落到配置 {@code tts.voice}）
 * @param sampleRate 目标采样率（Hz），用于音频编码参数对齐
 */
public record TtsContext(String sessionId, String voice, int sampleRate) {
}
