package com.ai.manager.system.iot.audio;

import lombok.extern.slf4j.Slf4j;
import org.concentus.OpusApplication;
import org.concentus.OpusDecoder;
import org.concentus.OpusEncoder;
import org.concentus.OpusException;
import org.concentus.OpusPacketInfo;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Opus 编解码封装（基于 org.concentus 纯 Java 实现，免 JNI）。
 * <p>
 * 仅支持 16k / 24k 单声道。编码固定 20ms 帧长，输出为「2 字节大端长度前缀 + Opus 包」的分帧流；
 * 解码优先按该分帧流解析，若首字节不满足分帧格式（设备一帧一个 WS binary 消息的裸 Opus 包），
 * 则回退为「整个输入按单个 Opus 包解码」。
 */
@Slf4j
@Component
public class OpusAudioCodec {

    /** 单帧时长 20ms（设备协议与 ESP 固件默认值） */
    public static final int FRAME_DURATION_MS = 20;

    /** 支持的采样率集合 */
    private static final int[] SUPPORTED_SAMPLE_RATES = {16000, 24000};

    private static final int DEFAULT_BITRATE = 24000;

    /**
     * PCM 采样 → Opus 分帧流。
     *
     * @param pcm        PCM 采样序列（单声道），尾部不足一帧自动补零
     * @param sampleRate 采样率 Hz（仅 16000 / 24000）
     * @return 分帧 Opus 字节（2 字节大端长度前缀 + 每帧 Opus 包）
     */
    public byte[] encodePcm(short[] pcm, int sampleRate) {
        validateSampleRate(sampleRate);
        if (pcm == null || pcm.length == 0) {
            return new byte[0];
        }
        int frameSize = samplesPerFrame(sampleRate);
        int totalSamples = roundUp(pcm.length, frameSize);
        try {
            OpusEncoder encoder = new OpusEncoder(sampleRate, 1, OpusApplication.OPUS_APPLICATION_VOIP);
            encoder.setBitrate(DEFAULT_BITRATE);
            byte[] frameBuf = new byte[4096];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            short[] frame = new short[frameSize];
            for (int off = 0; off < totalSamples; off += frameSize) {
                Arrays.fill(frame, (short) 0);
                int copyLen = Math.min(frameSize, pcm.length - off);
                if (copyLen > 0) {
                    System.arraycopy(pcm, off, frame, 0, copyLen);
                }
                int n = encoder.encode(frame, 0, frameSize, frameBuf, 0, frameBuf.length);
                writeFrame(out, frameBuf, n);
            }
            return out.toByteArray();
        } catch (OpusException e) {
            log.error("Opus 编码失败 sampleRate={}, pcmLength={}", sampleRate, pcm.length, e);
            throw new IllegalStateException("Opus 编码失败", e);
        }
    }

    /**
     * Opus → PCM 采样。
     *
     * @param opus       Opus 分帧流（本封装产出）或单个裸 Opus 包（设备一帧一消息）
     * @param sampleRate 采样率 Hz（仅 16000 / 24000）
     * @return 解码后的 PCM 采样序列（单声道）
     */
    public short[] decodeToPcm(byte[] opus, int sampleRate) {
        validateSampleRate(sampleRate);
        if (opus == null || opus.length == 0) {
            return new short[0];
        }
        try {
            OpusDecoder decoder = new OpusDecoder(sampleRate, 1);
            if (isFramedStream(opus)) {
                return decodeFramed(decoder, opus);
            }
            return decodeSingle(decoder, opus);
        } catch (OpusException e) {
            log.error("Opus 解码失败 sampleRate={}, opusLength={}", sampleRate, opus.length, e);
            throw new IllegalStateException("Opus 解码失败", e);
        }
    }

    private short[] decodeFramed(OpusDecoder decoder, byte[] opus) throws OpusException {
        // 第一遍扫描统计总样本数，一次性分配，避免频繁扩容
        int totalSamples = 0;
        int off = 0;
        while (off < opus.length) {
            int len = frameLength(opus, off);
            totalSamples += OpusPacketInfo.getNumSamples(decoder, opus, off + 2, len);
            off += 2 + len;
        }
        short[] pcm = new short[totalSamples];
        int pcmOff = 0;
        off = 0;
        while (off < opus.length) {
            int len = frameLength(opus, off);
            int numSamples = OpusPacketInfo.getNumSamples(decoder, opus, off + 2, len);
            int decoded = decoder.decode(opus, off + 2, len, pcm, pcmOff, numSamples, false);
            pcmOff += decoded;
            off += 2 + len;
        }
        return Arrays.copyOf(pcm, pcmOff);
    }

    private short[] decodeSingle(OpusDecoder decoder, byte[] opus) throws OpusException {
        int numSamples = OpusPacketInfo.getNumSamples(decoder, opus, 0, opus.length);
        if (numSamples <= 0) {
            // 极端兜底：按 60ms 单包最大缓冲分配，解码器会返回实际样本数
            numSamples = 1920;
        }
        short[] pcm = new short[numSamples];
        int decoded = decoder.decode(opus, 0, opus.length, pcm, 0, numSamples, false);
        return Arrays.copyOf(pcm, decoded);
    }

    /**
     * 校验输入是否为合法的分帧流：每帧长度前缀需能精确覆盖到缓冲末尾，且每帧内容可解析为 Opus 包。
     */
    private boolean isFramedStream(byte[] opus) {
        int off = 0;
        while (off < opus.length) {
            if (opus.length - off < 2) {
                return false;
            }
            int len = frameLength(opus, off);
            if (len <= 0 || off + 2 + len > opus.length) {
                return false;
            }
            if (OpusPacketInfo.getNumFrames(opus, off + 2, len) <= 0) {
                return false;
            }
            off += 2 + len;
        }
        return true;
    }

    private int frameLength(byte[] opus, int off) {
        return ((opus[off] & 0xff) << 8) | (opus[off + 1] & 0xff);
    }

    private void writeFrame(ByteArrayOutputStream out, byte[] data, int len) {
        out.write((len >> 8) & 0xff);
        out.write(len & 0xff);
        out.write(data, 0, len);
    }

    private int samplesPerFrame(int sampleRate) {
        return sampleRate / (1000 / FRAME_DURATION_MS);
    }

    private int roundUp(int value, int multiple) {
        return (value + multiple - 1) / multiple * multiple;
    }

    private void validateSampleRate(int sampleRate) {
        for (int supported : SUPPORTED_SAMPLE_RATES) {
            if (supported == sampleRate) {
                return;
            }
        }
        throw new IllegalArgumentException("不支持的 Opus 采样率: " + sampleRate + "，仅支持 16000/24000");
    }
}
