package com.ai.manager.system.iot.audio;

import java.nio.charset.StandardCharsets;

/**
 * WAV（RIFF）工具：PCM → WAV 封装与 WAV → PCM 解析。
 * <p>
 * 纯逻辑、无外部依赖，可单测。负责写/读 44 字节标准 RIFF 头（PCM 格式，16bit 为主）。
 */
public final class WavUtil {

    /** 标准 44 字节 PCM WAV 头大小 */
    public static final int HEADER_SIZE = 44;

    private WavUtil() {
    }

    /**
     * PCM 采样 → WAV 字节（16bit 为主，8bit 亦可）。
     *
     * @param pcm           PCM 采样序列（单声道逐样本，或已交织的多声道样本序列）
     * @param sampleRate    采样率 Hz，必须为正数
     * @param channels      声道数，必须为正数
     * @param bitsPerSample 位深，仅支持 8 或 16
     */
    public static byte[] pcmToWav(short[] pcm, int sampleRate, int channels, int bitsPerSample) {
        if (pcm == null) {
            throw new IllegalArgumentException("pcm 不能为空");
        }
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate 必须为正数");
        }
        if (channels <= 0) {
            throw new IllegalArgumentException("channels 必须为正数");
        }
        if (bitsPerSample != 8 && bitsPerSample != 16) {
            throw new IllegalArgumentException("仅支持 8/16bit PCM，当前: " + bitsPerSample);
        }
        int bytesPerSample = bitsPerSample / 8;
        long dataSizeLong = (long) pcm.length * bytesPerSample;
        if (dataSizeLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("PCM 数据过大，无法封装为 WAV");
        }
        int dataSize = (int) dataSizeLong;

        byte[] out = new byte[HEADER_SIZE + dataSize];
        writeAscii(out, 0, "RIFF");
        writeIntLe(out, 4, 36 + dataSize);
        writeAscii(out, 8, "WAVE");
        writeAscii(out, 12, "fmt ");
        writeIntLe(out, 16, 16);
        writeShortLe(out, 20, (short) 1); // PCM 编码
        writeShortLe(out, 22, (short) channels);
        writeIntLe(out, 24, sampleRate);
        writeIntLe(out, 28, sampleRate * channels * bytesPerSample); // byte rate
        writeShortLe(out, 32, (short) (channels * bytesPerSample));  // block align
        writeShortLe(out, 34, (short) bitsPerSample);
        writeAscii(out, 36, "data");
        writeIntLe(out, 40, dataSize);

        int offset = HEADER_SIZE;
        if (bitsPerSample == 16) {
            for (short s : pcm) {
                out[offset++] = (byte) (s & 0xff);
                out[offset++] = (byte) ((s >> 8) & 0xff);
            }
        } else {
            for (short s : pcm) {
                out[offset++] = (byte) (((s >> 8) + 128) & 0xff);
            }
        }
        return out;
    }

    /**
     * 解析 WAV 头并抽取 PCM 采样。
     *
     * @param wav WAV 字节
     * @return 解析结果（采样率 / 声道 / 位深 / PCM 采样）
     */
    public static WavInfo parseWav(byte[] wav) {
        if (wav == null || wav.length < HEADER_SIZE) {
            throw new IllegalArgumentException("非法 WAV: 长度不足 44 字节");
        }
        if (!"RIFF".equals(readAscii(wav, 0, 4)) || !"WAVE".equals(readAscii(wav, 8, 4))) {
            throw new IllegalArgumentException("非法 WAV: 缺少 RIFF/WAVE 标识");
        }
        int channels = readShortLe(wav, 22);
        int sampleRate = readIntLe(wav, 24);
        int bitsPerSample = readShortLe(wav, 34);
        int dataSize = readIntLe(wav, 40);
        int bytesPerSample = bitsPerSample / 8;
        if (bytesPerSample < 1) {
            throw new IllegalArgumentException("非法 WAV: 位深异常 " + bitsPerSample);
        }
        if (dataSize < 0 || (long) HEADER_SIZE + dataSize > wav.length) {
            throw new IllegalArgumentException("非法 WAV: data 长度越界");
        }
        int sampleCount = dataSize / bytesPerSample;
        short[] pcm = new short[sampleCount];
        int off = HEADER_SIZE;
        if (bitsPerSample == 16) {
            for (int i = 0; i < sampleCount; i++) {
                pcm[i] = (short) ((wav[off] & 0xff) | (wav[off + 1] << 8));
                off += 2;
            }
        } else if (bitsPerSample == 8) {
            for (int i = 0; i < sampleCount; i++) {
                pcm[i] = (short) (((wav[off] & 0xff) - 128) << 8);
                off += 1;
            }
        } else {
            throw new IllegalArgumentException("非法 WAV: 仅支持 8/16bit PCM");
        }
        return new WavInfo(sampleRate, channels, bitsPerSample, pcm);
    }

    /**
     * WAV → PCM 采样的便捷方法（仅取采样，忽略头信息）。
     */
    public static short[] wavToPcm(byte[] wav) {
        return parseWav(wav).pcm();
    }

    private static void writeAscii(byte[] out, int offset, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, out, offset, bytes.length);
    }

    private static String readAscii(byte[] wav, int offset, int len) {
        return new String(wav, offset, len, StandardCharsets.US_ASCII);
    }

    private static void writeIntLe(byte[] out, int offset, int value) {
        out[offset] = (byte) (value & 0xff);
        out[offset + 1] = (byte) ((value >> 8) & 0xff);
        out[offset + 2] = (byte) ((value >> 16) & 0xff);
        out[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    private static int readIntLe(byte[] wav, int offset) {
        return (wav[offset] & 0xff)
                | ((wav[offset + 1] & 0xff) << 8)
                | ((wav[offset + 2] & 0xff) << 16)
                | ((wav[offset + 3] & 0xff) << 24);
    }

    private static void writeShortLe(byte[] out, int offset, short value) {
        out[offset] = (byte) (value & 0xff);
        out[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    private static int readShortLe(byte[] wav, int offset) {
        return (wav[offset] & 0xff) | ((wav[offset + 1] & 0xff) << 8);
    }

    /**
     * WAV 头解析结果。
     *
     * @param sampleRate    采样率 Hz
     * @param channels      声道数
     * @param bitsPerSample 位深
     * @param pcm           PCM 采样序列
     */
    public record WavInfo(int sampleRate, int channels, int bitsPerSample, short[] pcm) {
    }
}
