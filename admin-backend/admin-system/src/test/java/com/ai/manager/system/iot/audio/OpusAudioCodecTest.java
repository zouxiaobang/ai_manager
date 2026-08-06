package com.ai.manager.system.iot.audio;

import org.concentus.OpusApplication;
import org.concentus.OpusEncoder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpusAudioCodecTest {

    private final OpusAudioCodec codec = new OpusAudioCodec();

    @Test
    void encodeDecode_roundTrip_16k() {
        short[] pcm = tone(16000, 16000 / 50 * 3); // 3 帧 × 20ms
        byte[] opus = codec.encodePcm(pcm, 16000);
        assertThat(opus).isNotEmpty();

        short[] decoded = codec.decodeToPcm(opus, 16000);
        assertThat(decoded).hasSize(pcm.length);
        assertThat(decoded).isNotEmpty();
    }

    @Test
    void encodeDecode_roundTrip_24k() {
        short[] pcm = tone(24000, 24000 / 50 * 2);
        byte[] opus = codec.encodePcm(pcm, 24000);
        assertThat(opus).isNotEmpty();

        short[] decoded = codec.decodeToPcm(opus, 24000);
        assertThat(decoded).hasSize(pcm.length);
        assertThat(decoded).isNotEmpty();
    }

    @Test
    void encodePcm_whenTailNotMultipleOfFrame_shouldPadAndRoundTrip() {
        // 长度不是 20ms 帧的整数倍（尾部补零）
        short[] pcm = tone(16000, 500);
        byte[] opus = codec.encodePcm(pcm, 16000);
        short[] decoded = codec.decodeToPcm(opus, 16000);
        assertThat(decoded).hasSize(640); // 补齐到 2 帧
        assertThat(decoded).isNotEmpty();
    }

    @Test
    void decode_singleRawOpusPacket_shouldDecode() throws Exception {
        // 用 Concentus 直接产出一个「不带长度前缀」的裸 Opus 包（模拟设备一帧一消息）
        short[] pcm = tone(16000, 320);
        OpusEncoder encoder = new OpusEncoder(16000, 1, OpusApplication.OPUS_APPLICATION_VOIP);
        encoder.setBitrate(24000);
        byte[] buf = new byte[4096];
        int n = encoder.encode(pcm, 0, 320, buf, 0, buf.length);
        byte[] raw = Arrays.copyOf(buf, n);

        short[] decoded = codec.decodeToPcm(raw, 16000);

        assertThat(decoded).hasSize(320);
        assertThat(decoded).isNotEmpty();
    }

    @Test
    void emptyInput_shouldReturnEmpty() {
        assertThat(codec.encodePcm(new short[0], 16000)).isEmpty();
        assertThat(codec.decodeToPcm(new byte[0], 16000)).isEmpty();
        assertThat(codec.encodePcm(null, 16000)).isEmpty();
        assertThat(codec.decodeToPcm(null, 16000)).isEmpty();
    }

    @Test
    void unsupportedSampleRate_shouldThrow() {
        assertThatThrownBy(() -> codec.encodePcm(new short[]{1}, 8000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("采样率");
        assertThatThrownBy(() -> codec.decodeToPcm(new byte[]{1}, 48000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("采样率");
    }

    private short[] tone(int sampleRate, int samples) {
        short[] pcm = new short[samples];
        for (int i = 0; i < samples; i++) {
            pcm[i] = (short) (Math.sin(2 * Math.PI * 440 * i / sampleRate) * 8000);
        }
        return pcm;
    }
}
