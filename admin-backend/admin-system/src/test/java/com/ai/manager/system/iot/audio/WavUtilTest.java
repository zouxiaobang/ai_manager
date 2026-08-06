package com.ai.manager.system.iot.audio;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WavUtilTest {

    @Test
    void pcmToWav_shouldWrite44ByteRiffHeader() {
        short[] pcm = new short[]{100, -100, 0, 32767, -32768};
        byte[] wav = WavUtil.pcmToWav(pcm, 16000, 1, 16);

        assertThat(wav).hasSize(44 + pcm.length * 2);
        assertThat(ascii(wav, 0, 4)).isEqualTo("RIFF");
        assertThat(ascii(wav, 8, 4)).isEqualTo("WAVE");
        assertThat(ascii(wav, 12, 4)).isEqualTo("fmt ");
        assertThat(ascii(wav, 36, 4)).isEqualTo("data");
        assertThat(readIntLe(wav, 24)).isEqualTo(16000);            // sample rate
        assertThat(readIntLe(wav, 28)).isEqualTo(16000 * 2);        // byte rate
        assertThat(readShortLe(wav, 22)).isEqualTo((short) 1);      // channels
        assertThat(readShortLe(wav, 34)).isEqualTo((short) 16);     // bits per sample
        assertThat(readShortLe(wav, 32)).isEqualTo((short) 2);      // block align
        assertThat(readIntLe(wav, 40)).isEqualTo(pcm.length * 2);   // data size
        // 首样本小端
        assertThat((short) ((wav[44] & 0xff) | (wav[45] << 8))).isEqualTo((short) 100);
    }

    @Test
    void wavToPcm_shouldRoundTripExactly() {
        short[] pcm = new short[]{1, -2, 300, -3000, 12345};
        byte[] wav = WavUtil.pcmToWav(pcm, 24000, 1, 16);

        WavUtil.WavInfo info = WavUtil.parseWav(wav);
        assertThat(info.sampleRate()).isEqualTo(24000);
        assertThat(info.channels()).isEqualTo(1);
        assertThat(info.bitsPerSample()).isEqualTo(16);
        assertThat(info.pcm()).containsExactly(pcm);

        assertThat(WavUtil.wavToPcm(wav)).containsExactly(pcm);
    }

    @Test
    void pcmToWav_8Bit_shouldEncodeUnsigned() {
        // 选 8bit 可无损表示的 16bit 值（256 的整数倍）
        short[] pcm = new short[]{0, 32512, -32768};
        byte[] wav = WavUtil.pcmToWav(pcm, 8000, 1, 8);
        assertThat(readShortLe(wav, 34)).isEqualTo((short) 8);
        // 8bit 按 unsigned 存，0 → 128
        assertThat(wav[44] & 0xff).isEqualTo(128);
        assertThat(WavUtil.wavToPcm(wav)).containsExactly(pcm);
    }

    @Test
    void invalidArguments_shouldThrow() {
        assertThatThrownBy(() -> WavUtil.pcmToWav(null, 16000, 1, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WavUtil.pcmToWav(new short[]{1}, 0, 1, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WavUtil.pcmToWav(new short[]{1}, 16000, 0, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WavUtil.pcmToWav(new short[]{1}, 16000, 1, 24))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseWav_invalidHeader_shouldThrow() {
        assertThatThrownBy(() -> WavUtil.parseWav(new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WavUtil.parseWav(new byte[44]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RIFF");
    }

    private String ascii(byte[] bytes, int offset, int len) {
        return new String(bytes, offset, len, StandardCharsets.US_ASCII);
    }

    private int readIntLe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 3] & 0xff) << 24);
    }

    private int readShortLe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }
}
