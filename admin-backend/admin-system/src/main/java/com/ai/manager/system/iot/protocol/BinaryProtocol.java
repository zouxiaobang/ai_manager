package com.ai.manager.system.iot.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 设备二进制协议编解码（v1/v2/v3，纯逻辑、零 Spring 依赖，可 host 单测）。
 * <p>
 * 与固件端 core/wire_format 对齐：
 * <ul>
 *   <li>v1：裸 Opus 帧，无头，负载即整帧。</li>
 *   <li>v2：头 = version(1B) + type(1B) + reserved(1B) + timestamp(4B 大端) + payload_size(2B 大端)，共 9 字节。</li>
 *   <li>v3：头 = type(1B) + reserved(1B) + payload_size(2B 大端)，共 4 字节（版本由握手协商，不入帧）。</li>
 * </ul>
 * 负载大小按 uint16 限制在 65535 字节内。
 * </p>
 */
public final class BinaryProtocol {

    public static final int V2_HEADER_LEN = 9;
    public static final int V3_HEADER_LEN = 4;

    private BinaryProtocol() {
    }

    /** v1：裸帧，直接拷贝负载。 */
    public static byte[] encodeV1(byte[] payload) {
        return Arrays.copyOf(payload, payload.length);
    }

    /** v2：version/type/reserved + 大端 timestamp + 大端 payload_size。 */
    public static byte[] encodeV2(int type, int reserved, long timestamp, byte[] payload) {
        requirePayloadSize(payload.length);
        byte[] frame = new byte[V2_HEADER_LEN + payload.length];
        frame[0] = (byte) ProtocolVersion.V2.code();
        frame[1] = (byte) type;
        frame[2] = (byte) reserved;
        writeInt32BE(frame, 3, timestamp);
        writeInt16BE(frame, 7, payload.length);
        System.arraycopy(payload, 0, frame, V2_HEADER_LEN, payload.length);
        return frame;
    }

    /** v3：type/reserved + 大端 payload_size（版本由握手协商，不入帧）。 */
    public static byte[] encodeV3(int type, int reserved, byte[] payload) {
        requirePayloadSize(payload.length);
        byte[] frame = new byte[V3_HEADER_LEN + payload.length];
        frame[0] = (byte) type;
        frame[1] = (byte) reserved;
        writeInt16BE(frame, 2, payload.length);
        System.arraycopy(payload, 0, frame, V3_HEADER_LEN, payload.length);
        return frame;
    }

    /** v1：整帧即负载。 */
    public static BinaryFrame decodeV1(byte[] frame) {
        byte[] payload = Arrays.copyOf(frame, frame.length);
        return new BinaryFrame(ProtocolVersion.V1.code(), 0, 0, 0L, payload);
    }

    /** v2：解析 9 字节头 + 负载。 */
    public static BinaryFrame decodeV2(byte[] frame) {
        if (frame == null || frame.length < V2_HEADER_LEN) {
            throw new IllegalArgumentException("v2 frame too short: " + (frame == null ? 0 : frame.length));
        }
        int version = frame[0] & 0xFF;
        int type = frame[1] & 0xFF;
        int reserved = frame[2] & 0xFF;
        long timestamp = readUInt32BE(frame, 3);
        int size = readUInt16BE(frame, 7);
        if (frame.length != V2_HEADER_LEN + size) {
            throw new IllegalArgumentException("v2 payload size mismatch: declared=" + size
                    + ", actual=" + (frame.length - V2_HEADER_LEN));
        }
        byte[] payload = Arrays.copyOfRange(frame, V2_HEADER_LEN, frame.length);
        return new BinaryFrame(version, type, reserved, timestamp, payload);
    }

    /** v3：解析 4 字节头 + 负载。 */
    public static BinaryFrame decodeV3(byte[] frame) {
        if (frame == null || frame.length < V3_HEADER_LEN) {
            throw new IllegalArgumentException("v3 frame too short: " + (frame == null ? 0 : frame.length));
        }
        int type = frame[0] & 0xFF;
        int reserved = frame[1] & 0xFF;
        int size = readUInt16BE(frame, 2);
        if (frame.length != V3_HEADER_LEN + size) {
            throw new IllegalArgumentException("v3 payload size mismatch: declared=" + size
                    + ", actual=" + (frame.length - V3_HEADER_LEN));
        }
        byte[] payload = Arrays.copyOfRange(frame, V3_HEADER_LEN, frame.length);
        return new BinaryFrame(ProtocolVersion.V3.code(), type, reserved, 0L, payload);
    }

    /** 按协商版本分发解码。 */
    public static BinaryFrame decode(int version, byte[] frame) {
        return switch (version) {
            case 1 -> decodeV1(frame);
            case 2 -> decodeV2(frame);
            case 3 -> decodeV3(frame);
            default -> throw new IllegalArgumentException("unsupported protocol version: " + version);
        };
    }

    private static void requirePayloadSize(int length) {
        if (length > 0xFFFF) {
            throw new IllegalArgumentException("payload too large for uint16: " + length);
        }
    }

    private static void writeInt16BE(byte[] dst, int offset, int value) {
        dst[offset] = (byte) (value >> 8);
        dst[offset + 1] = (byte) value;
    }

    private static void writeInt32BE(byte[] dst, int offset, long value) {
        dst[offset] = (byte) (value >> 24);
        dst[offset + 1] = (byte) (value >> 16);
        dst[offset + 2] = (byte) (value >> 8);
        dst[offset + 3] = (byte) value;
    }

    private static int readUInt16BE(byte[] src, int offset) {
        return ((src[offset] & 0xFF) << 8) | (src[offset + 1] & 0xFF);
    }

    private static long readUInt32BE(byte[] src, int offset) {
        return ((long) (src[offset] & 0xFF) << 24)
                | ((long) (src[offset + 1] & 0xFF) << 16)
                | ((long) (src[offset + 2] & 0xFF) << 8)
                | (long) (src[offset + 3] & 0xFF);
    }

    /** 便捷：将字符串按 UTF-8 编码为 v3 帧（文本消息用）。 */
    public static byte[] encodeV3Text(int type, int reserved, String text) {
        return encodeV3(type, reserved, text.getBytes(StandardCharsets.UTF_8));
    }
}
