#include "core/wifi_state.h"

namespace kyle {

WifiStateMachine::WifiStateMachine(WifiPolicy policy) : policy_(policy) {}

void WifiStateMachine::StartConnect() {
    state_ = WifiLinkState::kConnecting;
    retries_ = 0;
}

void WifiStateMachine::OnAssociated() {
    // 关联成功仍属连接中，等 IP（状态保持 kConnecting）
}

void WifiStateMachine::OnGotIp() {
    state_ = WifiLinkState::kConnected;
    retries_ = 0;
    has_connected_ = true;
}

void WifiStateMachine::OnDisconnected() {
    // 曾连上过：掉线视为瞬时，持续重连直至恢复（重试次数无上限）
    if (has_connected_) {
        state_ = WifiLinkState::kConnecting;
        ++retries_;
        return;
    }
    // 从未连上过：按 max_retries 限次，耗尽则回到未连接（等待重新配网/重试）
    if (should_retry()) {
        state_ = WifiLinkState::kConnecting;
        ++retries_;
    } else {
        state_ = WifiLinkState::kDisconnected;
    }
}

WifiLinkState WifiStateMachine::state() const { return state_; }

int WifiStateMachine::retry_count() const { return retries_; }

bool WifiStateMachine::should_retry() const { return retries_ < policy_.max_retries; }

int WifiStateMachine::retry_delay_ms() const {
    // 指数退避：基础间隔随重试次数翻倍，封顶 30s
    int delay = policy_.retry_delay_ms;
    for (int i = 0; i < retries_; ++i) {
        delay *= 2;
        if (delay >= 30000) return 30000;
    }
    return delay;
}

void WifiStateMachine::Reset() {
    state_ = WifiLinkState::kDisconnected;
    retries_ = 0;
    has_connected_ = false;
}

}  // namespace kyle
