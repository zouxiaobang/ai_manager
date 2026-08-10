// 极简入口：nvs → board_factory → Application → 事件循环
#include <cstdio>

#include "esp_log.h"
#include "nvs_flash.h"

#include "app/application.h"
#include "app/nvs_storage.h"
#include "boards/board_factory.h"

// K3 真机验证用：AUDIO_SELFTEST 开启时先播正弦 + 打印麦克风电平，再进入正常事件循环。
// K6 音频流水线接入后置 n，本段可整体删除。
#ifdef CONFIG_AUDIO_SELFTEST
#include <cmath>
#include <cstdint>
#include <vector>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

#include "hal/audio_codec.h"
#endif

static constexpr const char* kTag = "app_main";

#ifdef CONFIG_AUDIO_SELFTEST
namespace {
constexpr double kPi = 3.14159265358979323846;

void RunAudioSelfTest(kyle::IAudioCodec& audio) {
    constexpr int kSineFreqHz = 440;
    constexpr size_t kBlock = 480;  // 24k/480=50Hz 块（20ms）；16k 下约 30ms
    std::vector<int16_t> sine(kBlock);
    const double phase_step = 2.0 * kPi * kSineFreqHz / audio.output_sample_rate();
    for (size_t i = 0; i < kBlock; ++i) {
        // 振幅 8000（约 int16 满量程 24%），配合 70% 音量不会触发 ScaleTo32 钳位
        sine[i] = static_cast<int16_t>(8000.0 * std::sin(phase_step * static_cast<double>(i)));
    }
    std::vector<int16_t> mic(kBlock);

    audio.SetOutputVolume(70);
    if (!audio.Start()) {
        ESP_LOGE(kTag, "[K3] 音频启动失败");
        return;
    }

    // 阶段一：扬声器播正弦约 1.5s，确认出声
    ESP_LOGI(kTag, "[K3] 扬声器播放 440Hz 正弦 1.5s（应可闻）……");
    for (int block = 0; block < 75; ++block) {
        audio.Write(sine.data(), kBlock);
    }

    // 阶段二：循环打印麦克风峰值电平约 6s，说话/吹气可观察变化
    ESP_LOGI(kTag, "[K3] 采集麦克风电平 6s（说话/吹气观察峰值）……");
    for (int round = 0; round < 60; ++round) {
        const size_t n = audio.Read(mic.data(), kBlock);
        int32_t peak = 0;
        for (size_t i = 0; i < n; ++i) {
            const int32_t v = mic[i] < 0 ? -mic[i] : mic[i];
            if (v > peak) {
                peak = v;
            }
        }
        ESP_LOGI(kTag, "[K3] mic peak=%d", static_cast<int>(peak));
        vTaskDelay(pdMS_TO_TICKS(100));
    }
    audio.Stop();
}
}  // namespace
#endif

extern "C" void app_main(void) {
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    kyle::IBoard* board = kyle::CreateBoard();
    board->Init();
    ESP_LOGI(kTag, "board=%s target=%s", board->info().name, board->info().target);

    kyle::NvsStorage storage;
    kyle::Application app(*board, storage);
    app.Initialize();

#ifdef CONFIG_AUDIO_SELFTEST
    if (board->audio() != nullptr) {
        RunAudioSelfTest(*board->audio());
    }
#endif

    app.Run();  // 永不返回
}
