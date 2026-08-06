package com.ai.manager.system.iot.protocol;

/**
 * 设备二进制协议版本（握手时由 Protocol-Version 头协商）。
 * <p>
 * v1 裸 Opus 帧；v2 时间戳头；v3 简化头。与固件端 core/wire_format 对齐。
 * </p>
 */
public enum ProtocolVersion {

    V1(1),
    V2(2),
    V3(3);

    private final int code;

    ProtocolVersion(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ProtocolVersion fromCode(int code) {
        for (ProtocolVersion v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        throw new IllegalArgumentException("unsupported protocol version: " + code);
    }
}
