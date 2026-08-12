package com.ai.manager.system.iot.service;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.iot.asr.AsrContext;
import com.ai.manager.system.iot.asr.AsrProvider;
import com.ai.manager.system.iot.asr.AsrResult;
import com.ai.manager.system.iot.audio.OpusAudioCodec;
import com.ai.manager.system.iot.audio.WavUtil;
import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.tts.TtsContext;
import com.ai.manager.system.iot.tts.TtsProvider;
import com.ai.manager.system.iot.tts.TtsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 语音流水线编排（骨架）：一次语音轮次的完整链路。
 * <p>
 * 链路：设备 Opus → 解码 → WAV → ASR（文字）→（LLM 钩子，TODO）→ TTS（音频）→ 解码为 PCM → Opus 编码 → 下行。
 * LLM 回复目前由 {@link #generateReply(String)} 占位（原样回显），后续接入 claude-relay / LlmProviderStrategy，
 * 多轮上下文与流式输出不阻塞本次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoicePipelineService {

    /** 设备语音默认采样率（与 protocol.AudioParams 默认对齐） */
    private static final int DEFAULT_SAMPLE_RATE = 16000;

    private static final int CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 16;

    private final AsrProvider asrProvider;
    private final TtsProvider ttsProvider;
    private final OpusAudioCodec opusAudioCodec;
    private final IotProperties properties;

    /**
     * 编排一轮语音对话。
     *
     * @param deviceOpus 设备上行 Opus 字节（一帧或一帧集）
     * @param sessionId  会话 id
     * @return 下行 Opus 字节（编码为设备侧采样率，由设备播放）
     */
    public byte[] processTurn(byte[] deviceOpus, String sessionId) {
        int sampleRate = DEFAULT_SAMPLE_RATE;

        // 回显模式：跳过 ASR/TTS，先验证编解码链路（设备 Opus 上行 → 解码 → 重新编码 → 下行）
        if (properties.getVoice().isEchoMode()) {
            return echoTurn(deviceOpus, sessionId, sampleRate);
        }

        short[] pcm = opusAudioCodec.decodeToPcm(deviceOpus, sampleRate);
        byte[] wav = WavUtil.pcmToWav(pcm, sampleRate, CHANNELS, BITS_PER_SAMPLE);

        AsrResult asr = asrProvider.transcribe(wav,
                new AsrContext(sessionId, properties.getAsr().getLanguage(), sampleRate));
        log.info("ASR 完成 sessionId={}, text={}", sessionId, asr.text());

        String reply = generateReply(asr.text());

        TtsResult tts = ttsProvider.synthesize(reply,
                new TtsContext(sessionId, properties.getTts().getVoice(), sampleRate));
        log.info("TTS 完成 sessionId={}, format={}", sessionId, tts.format());

        short[] ttsPcm = toPcm(tts, sampleRate);
        return opusAudioCodec.encodePcm(ttsPcm, sampleRate);
    }

    /**
     * 回显模式：设备 Opus → 解码为 PCM → 重新编码为 Opus 原样下发。
     * 用于 K6 阶段真机验证编解码链路（ASR/TTS 未配置时 base-url 为空，调用会直接异常）。
     * <p>
     * 隔离实验（2026-08-12）：透传已证明后端重编码不是「沙沙」根因（透传仍沙沙），
     * 恢复 decode→encode，并顺带把上行解码 PCM dump 成 wav 供听/分析上行内容是否干净：
     * 清晰 = 下行 I2S 播放问题；沙沙 = 上行（麦克风/AFE/编码）问题。
     */
    private byte[] echoTurn(byte[] deviceOpus, String sessionId, int sampleRate) {
        short[] pcm = opusAudioCodec.decodeToPcm(deviceOpus, sampleRate);
        log.info("语音回显 sessionId={}, bytes={}, pcmSamples={}", sessionId, deviceOpus.length, pcm.length);
        dumpEchoWav(pcm, sampleRate, sessionId);
        return opusAudioCodec.encodePcm(pcm, sampleRate);
    }

    /**
     * 诊断 dump：把本段上行解码的 PCM 存为 16k 单声道 wav（C:/temp/echo_up_*.wav），
     * 用于区分「沙沙声」是上行采集/编码问题，还是下行播放问题。
     */
    private void dumpEchoWav(short[] pcm, int sampleRate, String sessionId) {
        try {
            Path dir = Paths.get("C:/temp/");
            Files.createDirectories(dir);
            // 会话 id 足够长才截前 8 位，短 id（如测试）原样用，避免 substring 越界
            String tag = sessionId.length() >= 8 ? sessionId.substring(0, 8) : sessionId;
            Path out = dir.resolve("echo_up_" + tag + ".wav");
            Files.write(out, WavUtil.pcmToWav(pcm, sampleRate, 1, 16));
            log.info("上行音频已 dump: {}", out.toAbsolutePath());
        } catch (Exception e) {
            // dump 失败不阻塞回显主链路（诊断辅助功能）
            log.warn("上行音频 dump 失败: {}", e.getMessage());
        }
    }

    /**
     * LLM 回复钩子（骨架占位）。
     * <p>
     * TODO: 接入 claude-relay / LlmProviderStrategy：把 {@code userText} 送入多轮对话并取回复，
     * 支持流式输出与打断控制。当前原样回显，保证 ASR→TTS 全链路可跑通。
     */
    protected String generateReply(String userText) {
        return userText;
    }

    /**
     * TTS 音频 → PCM。骨架阶段只支持 wav（可直接解析），opus 容器（Ogg）需后续接解封装。
     */
    private short[] toPcm(TtsResult tts, int sampleRate) {
        if ("wav".equalsIgnoreCase(tts.format())) {
            return WavUtil.wavToPcm(tts.audioBytes());
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(),
                "暂不支持的 TTS 返回格式: " + tts.format() + "，请配置 response-format=wav");
    }
}
