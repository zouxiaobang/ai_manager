package com.ai.manager.system.iot.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 二进制帧解码结果（纯逻辑，可单测）。
 */
@Getter
@AllArgsConstructor
public class BinaryFrame {

    private final int version;

    private final int type;

    private final int reserved;

    /** v2 帧才有有效时间戳，v1/v3 为 0 */
    private final long timestamp;

    private final byte[] payload;

    @Override
    public String toString() {
        return "BinaryFrame{version=" + version + ", type=" + type
                + ", reserved=" + reserved + ", timestamp=" + timestamp
                + ", payloadLen=" + (payload == null ? 0 : payload.length) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BinaryFrame that)) {
            return false;
        }
        return version == that.version && type == that.type
                && reserved == that.reserved && timestamp == that.timestamp
                && Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        int result = version;
        result = 31 * result + type;
        result = 31 * result + reserved;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}
