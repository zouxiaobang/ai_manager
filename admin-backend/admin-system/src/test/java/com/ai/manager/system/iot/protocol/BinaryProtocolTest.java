package com.ai.manager.system.iot.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BinaryProtocol v1/v2/v3 编解码黄金字节测试。
 */
class BinaryProtocolTest {

    @Test
    void encodeV1_shouldBeBarePayload() {
        byte[] payload = new byte[]{0x01, (byte) 0xAB};
        byte[] frame = BinaryProtocol.encodeV1(payload);
        assertThat(frame).containsExactly(0x01, (byte) 0xAB);
    }

    @Test
    void encodeV2_shouldProduceGoldenBytes() {
        byte[] payload = new byte[]{0x01, 0x02};
        byte[] frame = BinaryProtocol.encodeV2(0xAB, 0x0F, 0x12345678L, payload);
        // version(02) type(AB) reserved(0F) timestamp(12 34 56 78) size(00 02) payload(01 02)
        assertThat(frame).containsExactly(
                0x02, (byte) 0xAB, 0x0F,
                0x12, 0x34, 0x56, 0x78,
                0x00, 0x02,
                0x01, 0x02);
        assertThat(frame).hasSize(9 + 2);
    }

    @Test
    void encodeV3_shouldProduceGoldenBytes() {
        byte[] payload = new byte[]{0x01, 0x02};
        byte[] frame = BinaryProtocol.encodeV3(0xAB, 0x0F, payload);
        // type(AB) reserved(0F) size(00 02) payload(01 02)
        assertThat(frame).containsExactly((byte) 0xAB, 0x0F, 0x00, 0x02, 0x01, 0x02);
        assertThat(frame).hasSize(4 + 2);
    }

    @Test
    void encode_shouldRejectOversizePayload() {
        byte[] big = new byte[0x10000];
        assertThatThrownBy(() -> BinaryProtocol.encodeV2(0, 0, 0, big))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload too large");
        assertThatThrownBy(() -> BinaryProtocol.encodeV3(0, 0, big))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decodeV1_shouldWrapWholeFrameAsPayload() {
        byte[] frame = new byte[]{0x11, 0x22, 0x33};
        BinaryFrame decoded = BinaryProtocol.decodeV1(frame);
        assertThat(decoded.getVersion()).isEqualTo(1);
        assertThat(decoded.getType()).isZero();
        assertThat(decoded.getTimestamp()).isZero();
        assertThat(decoded.getPayload()).containsExactly(0x11, 0x22, 0x33);
    }

    @Test
    void decodeV2_shouldParseGoldenBytes() {
        byte[] frame = new byte[]{0x02, (byte) 0xAB, 0x0F, 0x12, 0x34, 0x56, 0x78, 0x00, 0x02, 0x01, 0x02};
        BinaryFrame decoded = BinaryProtocol.decodeV2(frame);
        assertThat(decoded.getVersion()).isEqualTo(2);
        assertThat(decoded.getType()).isEqualTo(0xAB);
        assertThat(decoded.getReserved()).isEqualTo(0x0F);
        assertThat(decoded.getTimestamp()).isEqualTo(0x12345678L);
        assertThat(decoded.getPayload()).containsExactly(0x01, 0x02);
    }

    @Test
    void decodeV3_shouldParseGoldenBytes() {
        byte[] frame = new byte[]{(byte) 0xAB, 0x0F, 0x00, 0x02, 0x01, 0x02};
        BinaryFrame decoded = BinaryProtocol.decodeV3(frame);
        assertThat(decoded.getVersion()).isEqualTo(3);
        assertThat(decoded.getType()).isEqualTo(0xAB);
        assertThat(decoded.getReserved()).isEqualTo(0x0F);
        assertThat(decoded.getTimestamp()).isZero();
        assertThat(decoded.getPayload()).containsExactly(0x01, 0x02);
    }

    @Test
    void decode_shouldDispatchByVersion() {
        byte[] v2 = new byte[]{0x02, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x55};
        BinaryFrame d2 = BinaryProtocol.decode(2, v2);
        assertThat(d2.getVersion()).isEqualTo(2);
        assertThat(d2.getPayload()).containsExactly(0x55);

        byte[] v3 = new byte[]{0x01, 0x00, 0x00, 0x01, 0x55};
        BinaryFrame d3 = BinaryProtocol.decode(3, v3);
        assertThat(d3.getVersion()).isEqualTo(3);
        assertThat(d3.getType()).isEqualTo(1);

        byte[] v1 = new byte[]{0x55, 0x66};
        BinaryFrame d1 = BinaryProtocol.decode(1, v1);
        assertThat(d1.getVersion()).isEqualTo(1);
        assertThat(d1.getPayload()).containsExactly(0x55, 0x66);
    }

    @Test
    void decode_shouldRejectUnsupportedVersion() {
        assertThatThrownBy(() -> BinaryProtocol.decode(4, new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decodeV2_shouldRejectShortFrame() {
        assertThatThrownBy(() -> BinaryProtocol.decodeV2(new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too short");
    }

    @Test
    void decodeV2_shouldRejectSizeMismatch() {
        byte[] frame = new byte[]{0x02, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x01};
        assertThatThrownBy(() -> BinaryProtocol.decodeV2(frame))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size mismatch");
    }

    @Test
    void decodeV3_shouldRejectSizeMismatch() {
        byte[] frame = new byte[]{0x01, 0x00, 0x00, 0x05, 0x01};
        assertThatThrownBy(() -> BinaryProtocol.decodeV3(frame))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size mismatch");
    }

    @Test
    void encodeV2_roundTripShouldBeStable() {
        byte[] payload = "hello opus".getBytes();
        byte[] frame = BinaryProtocol.encodeV2(7, 0, 0x01020304L, payload);
        BinaryFrame decoded = BinaryProtocol.decodeV2(frame);
        assertThat(decoded.getType()).isEqualTo(7);
        assertThat(decoded.getTimestamp()).isEqualTo(0x01020304L);
        assertThat(decoded.getPayload()).isEqualTo(payload);
    }

    @Test
    void encodeV3Text_shouldEncodeUtf8Payload() {
        byte[] frame = BinaryProtocol.encodeV3Text(3, 0, "你好");
        BinaryFrame decoded = BinaryProtocol.decodeV3(frame);
        assertThat(new String(decoded.getPayload(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("你好");
    }
}
