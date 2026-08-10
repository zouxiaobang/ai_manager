#ifndef KYLE_APP_APPLICATION_H
#define KYLE_APP_APPLICATION_H

#include <deque>
#include <functional>
#include <memory>

#include "core/chat_session.h"
#include "core/net_config.h"
#include "hal/board.h"

namespace kyle {

// 业务编排骨架：输入事件 → ChatSession 状态机，消息/配置 → wire_format / NetConfig。
// 保持纯 C++，不依赖 ESP-IDF；真正的 NVS 存储由 app_main 注入 NvsStorage。
class Application {
public:
    Application(IBoard& board, IStorage& storage);
    ~Application() = default;

    void Initialize();
    void Run();

    // 跨任务/回调安全投递到主循环（入队）
    void Schedule(std::function<void()> fn);

    ChatSession& session() { return session_; }
    NetConfig& net_config() { return *net_config_; }

private:
    void DispatchPendingEvents();
    void OnInputEvent(const InputEvent& ev);
    void ToggleSession();
    void AdjustVolume(int delta);
    void UpdateLedForSession();

    IBoard& board_;
    IStorage& storage_;
    std::unique_ptr<NetConfig> net_config_;
    ChatSession session_;
    std::deque<std::function<void()>> queue_;
    bool running_ = true;
    int volume_ = 70;  // 与 NoCodecI2s 默认音量一致
};

}  // namespace kyle

#endif  // KYLE_APP_APPLICATION_H
