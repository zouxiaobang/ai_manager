#ifndef KYLE_CORE_WIFI_STATE_H
#define KYLE_CORE_WIFI_STATE_H

// WiFi 连接状态机：由 NetworkEsp 驱动，输入 esp_wifi 事件，输出状态与重连决策。
// 纯逻辑，零 ESP-IDF 依赖，可在 host 单测（决定"何时重试/退避多久"）。

namespace kyle {

// 链路状态（与 hal WifiState 一一对应；core 不依赖 hal，故独立定义，由驱动层映射）
enum class WifiLinkState {
    kDisconnected,  // 未连接 / 未配网
    kConnecting,    // 正在连接
    kConnected,     // 已连接且拿到 IP
};

// 连接策略（真机可按需调整）
struct WifiPolicy {
    int max_retries = 5;      // 断线/失败后最多重连次数
    int retry_delay_ms = 3000;  // 基础重连间隔
};

class WifiStateMachine {
public:
    explicit WifiStateMachine(WifiPolicy policy = WifiPolicy());

    // ---- 输入事件（NetworkEsp 事件回调调用）----
    // 有凭据，发起连接（重置重试计数）
    void StartConnect();
    // STA 关联成功（仍在连接中，等 IP）
    void OnAssociated();
    // 拿到 IP → kConnected
    void OnGotIp();
    // 断线 / 连接失败（若还有重试机会回 kConnecting，否则 kDisconnected）
    void OnDisconnected();

    // ---- 查询 ----
    WifiLinkState state() const;
    int retry_count() const;
    bool should_retry() const;       // retries < max_retries
    int retry_delay_ms() const;      // 重连前应等待的时长

    void Reset();

private:
    WifiPolicy policy_;
    WifiLinkState state_ = WifiLinkState::kDisconnected;
    int retries_ = 0;
    // 是否曾成功连接过：已连过则掉线视为瞬时（继续自动重连），
    // 从未连上过则按 max_retries 限制尝试次数，耗尽后回到未连接态。
    bool has_connected_ = false;
};

}  // namespace kyle

#endif  // KYLE_CORE_WIFI_STATE_H
