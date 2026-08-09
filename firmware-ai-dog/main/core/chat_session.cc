#include "core/chat_session.h"

namespace kyle {

bool ChatSession::IsValidTransition(SessionState from, SessionEvent ev) {
    switch (from) {
        case kIdle:
            return ev == kStart || ev == kWakeWordDetected;
        case kConnecting:
            return ev == kChannelOpened || ev == kError || ev == kDisconnected ||
                   ev == kStopListening;
        case kListening:
            return ev == kSpeakingStarted || ev == kStopListening || ev == kError ||
                   ev == kDisconnected;
        case kSpeaking:
            return ev == kSpeakingStopped || ev == kWakeWordDetected || ev == kStopListening ||
                   ev == kError || ev == kDisconnected;
    }
    return false;
}

bool ChatSession::HandleEvent(SessionEvent ev) {
    if (!IsValidTransition(state_, ev)) return false;  // 非法转移拒绝

    AbortReason reason = kAbortNone;
    switch (state_) {
        case kIdle:
            state_ = kConnecting;
            break;
        case kConnecting:
            if (ev == kChannelOpened) {
                state_ = kListening;
            } else {  // kError / kDisconnected / kStopListening
                state_ = kIdle;
                reason = kAbortUser;
            }
            break;
        case kListening:
            if (ev == kSpeakingStarted) {
                state_ = kSpeaking;
            } else {  // kStopListening / kError / kDisconnected
                state_ = kIdle;
                reason = kAbortUser;
            }
            break;
        case kSpeaking:
            if (ev == kSpeakingStopped) {
                state_ = kListening;          // TTS 结束自动续听
            } else if (ev == kWakeWordDetected) {
                state_ = kListening;          // barge-in：打断后回到监听
                reason = kAbortWakeWordDetected;
            } else {  // kStopListening / kError / kDisconnected
                state_ = kIdle;
                reason = kAbortUser;
            }
            break;
    }
    last_abort_reason_ = reason;
    return true;
}

}  // namespace kyle
