package com.ai.manager.system.iot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * IoT 设备功能域配置（ai-manager.iot.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-manager.iot")
public class IotProperties {

    /** 下发给设备的 WebSocket 地址（ESP 侧 wss 反代到 /ws/device） */
    private String wsUrl = "ws://127.0.0.1:8080/ws/device";

    /** 设备 WebSocket token 有效期（秒） */
    private long tokenTtlSeconds = 86400;

    /** 固件文件存储目录 */
    private String otaDir = "uploads/iot/firmware";

    /** OTA 固件下载 base url（用于拼接 download 链接） */
    private String otaBaseUrl = "http://127.0.0.1:8080/api/iot/ota/download";

    /** ASR（语音识别）提供方配置 */
    private Asr asr = new Asr();

    /** TTS（语音合成）提供方配置 */
    private Tts tts = new Tts();

    /** 激活挑战应答 HMAC-SHA256 密钥 */
    private String activationSecret = "ai-manager-iot-activation-secret";

    /** WebSocket 握手协议版本（默认 v3） */
    private int protocolVersion = 3;

    /** MQTT 通道 endpoint（未启用留空） */
    private String mqttEndpoint = "";

    /** MQTT 通道 client_id 前缀 */
    private String mqttClientIdPrefix = "iot-";

    /** MQTT 通道用户名 */
    private String mqttUsername = "";

    /** MQTT 通道密码 */
    private String mqttPassword = "";

    /** 设备时区偏移（分钟），默认东八区 */
    private int timezoneOffsetMinutes = 480;

    /**
     * ASR provider 配置（ai-manager.iot.asr.*）。
     * 换厂商只改 type / base-url / api-key，后端按 type 选择对应 AsrProvider 实现。
     */
    @Data
    public static class Asr {
        /** provider 类型：openai-compatible（OpenAI Whisper 兼容 /v1/audio/transcriptions） */
        private String type = "openai-compatible";

        /** 云 API base url（不含 /v1，如 https://api.openai.com），留空则请求不可用 */
        private String baseUrl = "";

        /** API key（换自建/本地服务可留空） */
        private String apiKey = "";

        /** Whisper 模型名 */
        private String model = "whisper-1";

        /** 识别语言（ISO-639，如 zh/zh-CN，留空由服务端自动判断） */
        private String language = "";
    }

    /**
     * TTS provider 配置（ai-manager.iot.tts.*）。
     * 换厂商只改 type / base-url / api-key，后端按 type 选择对应 TtsProvider 实现。
     */
    @Data
    public static class Tts {
        /** provider 类型：openai-compatible（OpenAI 兼容 /v1/audio/speech） */
        private String type = "openai-compatible";

        /** 云 API base url（不含 /v1，如 https://api.openai.com），留空则请求不可用 */
        private String baseUrl = "";

        /** API key（换自建/本地服务可留空） */
        private String apiKey = "";

        /** TTS 模型名 */
        private String model = "tts-1";

        /** 音色（OpenAI 内置如 alloy / nova，或自定义音色 id） */
        private String voice = "alloy";

        /** 返回音频格式：wav（走 后端解码→Opus 重编码）或 opus（Ogg Opus，需设备端支持） */
        private String responseFormat = "wav";
    }
}
