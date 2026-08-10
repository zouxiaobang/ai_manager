#include "app/application.h"

#include <cstdio>

#include "hal/input.h"

// Run() 需要让出主任务；host 单测环境不定义 ESP_PLATFORM，保持纯 C++。
#ifdef ESP_PLATFORM
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

namespace kyle {

Application::Application(IBoard& board, IStorage& storage)
    : board_(board), storage_(storage) {}

void Application::Initialize() {
    net_config_ = std::make_unique<NetConfig>(storage_);

    // 输入事件 → 会话状态机（触摸/按键统一走 OnInputEvent，按 button_id 分发）
    if (auto* input = board_.input()) {
        input->OnEvent([this](const InputEvent& ev) { OnInputEvent(ev); });
    }
    UpdateLedForSession();  // 初始空闲态

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
    // 长按 BOOT：进入深睡（物理按键专属；触摸长按不深睡）。
    // 关断序列（LED→音频→背光→屏→深睡）归板级 EnterSleep() 单点实现，
    // 应用层不感知硬件顺序（顺序敏感，见各板实现注释）。
    if (ev.type == InputEvent::kLongPress && ev.button_id == kButtonBoot) {
        board_.EnterSleep();
        return;
    }
    if (ev.type != InputEvent::kClick) {
        return;  // 双击/其他长按：K4 暂不映射动作
    }
    switch (ev.button_id) {
        case kButtonBoot:     // BOOT 单击：对话开关
        case kTouchButtonId:  // 触摸单击：对话开关
            ToggleSession();
            break;
        case kButtonVolUp:    // UP：音量 +
            AdjustVolume(+10);
            break;
        case kButtonVolDown:  // DOWN：音量 -
            AdjustVolume(-10);
            break;
        default:
            break;
    }
}

void Application::ToggleSession() {
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "单击: 会话 %s", session_.state() == kIdle ? "开始" : "停止");
#endif
    if (session_.state() == kIdle) {
        session_.OnEvent(kStart);
        // TODO(firmware): 用 net_config().websocket() 取 url/token，发 BuildHelloMessage
    } else {
        session_.OnEvent(kStopListening);
        // TODO(firmware): 发 BuildListenMessage(session_id, "stop")
    }
    UpdateLedForSession();
}

void Application::AdjustVolume(int delta) {
    int v = volume_ + delta;
    volume_ = (v < 0) ? 0 : (v > 100 ? 100 : v);
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "音量 %d%%", volume_);
#endif
    if (auto* a = board_.audio()) {
        a->SetOutputVolume(volume_);
    }
    // 音量变化给到屏显示（真机 St7789 的 ShowToast 当前为空实现，无副作用）
    if (auto* disp = board_.display()) {
        char buf[32];
        std::snprintf(buf, sizeof(buf), "音量 %d%%", volume_);
        disp->ShowToast(buf, 1500);
    }
}

void Application::UpdateLedForSession() {
    if (auto* led = board_.led()) {
        switch (session_.state()) {
            case kIdle:
                led->SetState(LedState::kLedIdle);
                break;
            case kConnecting:
                led->SetState(LedState::kLedConnecting);
                break;
            case kListening:
                led->SetState(LedState::kLedListening);
                break;
            case kSpeaking:
                led->SetState(LedState::kLedSpeaking);
                break;
        }
    }
}

}  // namespace kyle
