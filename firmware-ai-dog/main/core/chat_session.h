#ifndef KYLE_CORE_CHAT_SESSION_H
#define KYLE_CORE_CHAT_SESSION_H

// 会话状态机：idle / connecting / listening / speaking。
// 纯逻辑，零 ESP-IDF 依赖；合法转移矩阵由 IsValidTransition 给出，非法转移被拒绝。
// abort（如唤醒词打断）会记录 last_abort_reason，供协议层发送 abort 消息。

#include "core/wire_format.h"  // 复用 AbortReason

namespace kyle {

enum SessionState {
    kIdle,
    kConnecting,
    kListening,
    kSpeaking,
};

enum SessionEvent {
    kStart,              // 用户/唤醒词触发新会话
    kChannelOpened,      // 服务端 hello 收到、音频通道就绪
    kStopListening,      // 显式停止监听
    kSpeakingStarted,    // TTS 开始下发
    kSpeakingStopped,    // TTS 结束 → 自动续听
    kWakeWordDetected,   // 唤醒词打断（barge-in）
    kError,              // 网络/错误 → 复位
    kDisconnected,       // 通道断开 → 复位
};

class ChatSession {
public:
    // 处理事件；合法则应用转移并返回 true，非法返回 false 且状态不变
    bool HandleEvent(SessionEvent ev);
    void OnEvent(SessionEvent ev) { HandleEvent(ev); }

    void Reset() {
        state_ = kIdle;
        last_abort_reason_ = kAbortNone;
    }

    SessionState state() const { return state_; }
    bool IsActive() const { return state_ != kIdle; }
    AbortReason last_abort_reason() const { return last_abort_reason_; }

    // 合法转移矩阵（纯函数，供 host 测试逐格校验）
    static bool IsValidTransition(SessionState from, SessionEvent ev);

private:
    SessionState state_ = kIdle;
    AbortReason last_abort_reason_ = kAbortNone;
};

}  // namespace kyle

#endif  // KYLE_CORE_CHAT_SESSION_H
