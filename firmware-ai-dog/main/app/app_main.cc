// 极简入口：nvs → board_factory → Application → 事件循环
#include <cstdio>

#include "esp_log.h"
#include "nvs_flash.h"

#include "app/application.h"
#include "app/nvs_storage.h"
#include "boards/board_factory.h"

static constexpr const char* kTag = "app_main";

extern "C" void app_main(void) {
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    xiaozhi::IBoard* board = xiaozhi::CreateBoard();
    board->Init();
    ESP_LOGI(kTag, "board=%s target=%s", board->info().name, board->info().target);

    xiaozhi::NvsStorage storage;
    xiaozhi::Application app(*board, storage);
    app.Initialize();
    app.Run();  // 永不返回
}
