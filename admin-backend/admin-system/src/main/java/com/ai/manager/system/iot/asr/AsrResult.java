package com.ai.manager.system.iot.asr;

/**
 * ASR 识别结果。
 *
 * @param text       识别出的文字（空串表示未识别到语音）
 * @param raw        云端原始响应 JSON（便于排查 / 扩展）
 * @param durationMs 本次识别请求耗时（毫秒）
 */
public record AsrResult(String text, String raw, long durationMs) {
}
