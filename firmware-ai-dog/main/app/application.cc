#include "app/application.h"

#include "core/wire_format.h"

// Run() 需要让出主任务；host 单测环境不定义 ESP_PLATFORM，保持纯 C++。
#ifdef ESP_PLATFORM
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

namespace kyle {

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
        // 让出主任务，避免 IDLE0 饿死触发任务看门狗（真机 task_wdt 已实测触发）。
        // K5 网络回调/唤醒词就绪后改为事件驱动的阻塞等待，先定频轮询。
#ifdef ESP_PLATFORM
        vTaskDelay(pdMS_TO_TICKS(50));
#endif
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

}  // namespace kyle
