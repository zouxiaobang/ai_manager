#include "app/application.h"

#include "core/wire_format.h"

namespace xiaozhi {

Application::Application(IBoard& board, IStorage& storage)
    : board_(board), storage_(storage) {}

void Application::Initialize() {
    net_config_ = std::make_unique<NetConfig>(storage_);

    // 输入事件 → 会话状态机（骨架：单击 BOOT 切换会话开关）
    if (auto* input = board_.input()) {
        input->OnEvent([this](const InputEvent& ev) { OnInputEvent(ev); });
    }

    // TODO(firmware): 网络连接回调 → session_.OnEvent(kChannelOpened / kDisconnected)
    // TODO(firmware): 唤醒词触发 → session_.OnEvent(kWakeWordDetected) + 发 listen(detect)
    // TODO(firmware): 收到服务端 tts/llm → session_.OnEvent(kSpeakingStarted / kSpeakingStopped)
}

void Application::Schedule(std::function<void()> fn) {
    queue_.push_back(std::move(fn));
}

void Application::Run() {
    while (running_) {
        DispatchPendingEvents();
        // TODO(firmware): 接入 vTaskDelay(pdMS_TO_TICKS(50)) 让出主任务，纯 C++ 骨架先空转。
    }
}

void Application::DispatchPendingEvents() {
    while (!queue_.empty()) {
        auto fn = std::move(queue_.front());
        queue_.pop_front();
        if (fn) fn();
    }
}

void Application::OnInputEvent(const InputEvent& ev) {
    if (ev.type != InputEvent::kClick) return;
    if (session_.state() == kIdle) {
        session_.OnEvent(kStart);
        // TODO(firmware): 用 net_config().websocket() 取 url/token，发 BuildHelloMessage
    } else {
        session_.OnEvent(kStopListening);
        // TODO(firmware): 发 BuildListenMessage(session_id, "stop")
    }
}

}  // namespace xiaozhi
