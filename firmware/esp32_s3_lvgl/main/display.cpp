#include "display.h"

#include "esp_check.h"
#include "esp_heap_caps.h"
#include "esp_lcd_panel_ops.h"
#include "esp_lcd_panel_rgb.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "freertos/task.h"
#include "panel_config.h"
#include "sdkconfig.h"

#if PANEL_LVGL_ROTATION_180 && !CONFIG_LV_DRAW_TRANSFORM_USE_MATRIX
#error "PANEL_LVGL_ROTATION_180 requires CONFIG_LV_DRAW_TRANSFORM_USE_MATRIX=y (idf.py reconfigure)"
#endif

namespace {
constexpr char TAG[] = "display";
constexpr size_t kLvglTaskStack = 12288;
constexpr uint32_t kLvglTaskDelayMs = 2;
constexpr uint32_t kLvglTickPeriodMs = 2;
constexpr UBaseType_t kLvglTaskPriority = 4;

esp_lcd_panel_handle_t panel_handle = nullptr;
lv_display_t *lv_display = nullptr;
SemaphoreHandle_t lvgl_mutex = nullptr;
esp_timer_handle_t lvgl_tick_timer = nullptr;

void lvgl_flush(lv_display_t *disp, const lv_area_t *area, uint8_t *px_map) {
#if LVGL_USE_RGB_DOUBLE_FB
  (void)area;
  (void)px_map;
  lv_display_flush_ready(disp);
#else
  const int x1 = area->x1;
  const int y1 = area->y1;
  const int x2 = area->x2 + 1;
  const int y2 = area->y2 + 1;
  esp_lcd_panel_draw_bitmap(panel_handle, x1, y1, x2, y2, px_map);
  lv_display_flush_ready(disp);
  // 分块刷屏期间让出 CPU，避免长时间占用导致其他任务饥饿
  taskYIELD();
#endif
}

void lvgl_add_psram_mem_pool() {
  constexpr size_t kPoolBytes = 256 * 1024;
  void *pool = heap_caps_malloc(kPoolBytes, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT);
  if (pool == nullptr) {
    ESP_LOGW(TAG, "LVGL PSRAM pool alloc failed (%u KB)", static_cast<unsigned>(kPoolBytes / 1024));
    return;
  }
  if (lv_mem_add_pool(pool, kPoolBytes) == nullptr) {
    ESP_LOGW(TAG, "LVGL PSRAM pool register failed");
    heap_caps_free(pool);
    return;
  }
  ESP_LOGI(TAG, "LVGL PSRAM pool +%u KB", static_cast<unsigned>(kPoolBytes / 1024));
}

void lvgl_tick_cb(void *arg) {
  (void)arg;
  lv_tick_inc(kLvglTickPeriodMs);
}

void lvgl_task(void *arg) {
  (void)arg;
  // LVGL 刷新/绘制可能单次超过 TWDT 超时，勿订阅任务看门狗（IDLE 检查已在 sdkconfig 关闭）

  while (true) {
    display_lock();
    const uint32_t wait_ms = lv_timer_handler();
    display_unlock();

    uint32_t delay_ms = kLvglTaskDelayMs;
    if (wait_ms != LV_NO_TIMER_READY && wait_ms > 0) {
      delay_ms = wait_ms > 10 ? 10 : wait_ms;
    }
    vTaskDelay(pdMS_TO_TICKS(delay_ms));
  }
}
}  // namespace

esp_err_t display_init() {
  ESP_LOGI(TAG, "Initialize RGB panel");

  esp_lcd_rgb_panel_config_t panel_config = {};
  panel_config.clk_src = LCD_CLK_SRC_DEFAULT;
  panel_config.timings.pclk_hz = LCD_PIXEL_CLOCK_HZ;
  panel_config.timings.h_res = PANEL_WIDTH;
  panel_config.timings.v_res = PANEL_HEIGHT;
  panel_config.timings.hsync_pulse_width = HSYNC_PULSE_WIDTH;
  panel_config.timings.hsync_back_porch = HSYNC_BACK_PORCH;
  panel_config.timings.hsync_front_porch = HSYNC_FRONT_PORCH;
  panel_config.timings.vsync_pulse_width = VSYNC_PULSE_WIDTH;
  panel_config.timings.vsync_back_porch = VSYNC_BACK_PORCH;
  panel_config.timings.vsync_front_porch = VSYNC_FRONT_PORCH;
  panel_config.timings.flags.pclk_active_neg = PCLK_ACTIVE_NEG;
  panel_config.data_width = 16;
  panel_config.bits_per_pixel = 16;
#if LVGL_USE_RGB_DOUBLE_FB
  panel_config.num_fbs = 2;
  panel_config.bounce_buffer_size_px = RGB_BOUNCE_BUFFER_SIZE_PX;
#else
  panel_config.num_fbs = 1;
  panel_config.bounce_buffer_size_px = RGB_BOUNCE_BUFFER_SIZE_PX;
#endif
  panel_config.sram_trans_align = 64;
  panel_config.psram_trans_align = 64;
  panel_config.hsync_gpio_num = LCD_HSYNC;
  panel_config.vsync_gpio_num = LCD_VSYNC;
  panel_config.de_gpio_num = LCD_DE;
  panel_config.pclk_gpio_num = LCD_PCLK;
  panel_config.disp_gpio_num = -1;
  panel_config.data_gpio_nums[0] = LCD_B3;
  panel_config.data_gpio_nums[1] = LCD_B4;
  panel_config.data_gpio_nums[2] = LCD_B5;
  panel_config.data_gpio_nums[3] = LCD_B6;
  panel_config.data_gpio_nums[4] = LCD_B7;
  panel_config.data_gpio_nums[5] = LCD_G2;
  panel_config.data_gpio_nums[6] = LCD_G3;
  panel_config.data_gpio_nums[7] = LCD_G4;
  panel_config.data_gpio_nums[8] = LCD_G5;
  panel_config.data_gpio_nums[9] = LCD_G6;
  panel_config.data_gpio_nums[10] = LCD_G7;
  panel_config.data_gpio_nums[11] = LCD_R3;
  panel_config.data_gpio_nums[12] = LCD_R4;
  panel_config.data_gpio_nums[13] = LCD_R5;
  panel_config.data_gpio_nums[14] = LCD_R6;
  panel_config.data_gpio_nums[15] = LCD_R7;
  panel_config.flags.fb_in_psram = true;

  ESP_RETURN_ON_ERROR(esp_lcd_new_rgb_panel(&panel_config, &panel_handle), TAG, "Create RGB panel failed");
  ESP_RETURN_ON_ERROR(esp_lcd_panel_reset(panel_handle), TAG, "Reset RGB panel failed");
  ESP_RETURN_ON_ERROR(esp_lcd_panel_init(panel_handle), TAG, "Init RGB panel failed");

#if PANEL_MIRROR_X || PANEL_MIRROR_Y
  ESP_RETURN_ON_ERROR(esp_lcd_panel_mirror(panel_handle, PANEL_MIRROR_X, PANEL_MIRROR_Y), TAG,
                       "Mirror RGB panel failed");
  ESP_LOGI(TAG, "RGB panel mirror X=%d Y=%d", PANEL_MIRROR_X, PANEL_MIRROR_Y);
#endif

  lv_init();
  lvgl_add_psram_mem_pool();
  lvgl_mutex = xSemaphoreCreateRecursiveMutex();
  ESP_RETURN_ON_FALSE(lvgl_mutex != nullptr, ESP_ERR_NO_MEM, TAG, "Create LVGL mutex failed");

  const esp_timer_create_args_t tick_timer_args = {
    .callback = lvgl_tick_cb,
    .arg = nullptr,
    .dispatch_method = ESP_TIMER_TASK,
    .name = "lvgl_tick",
    .skip_unhandled_events = true,
  };
  ESP_RETURN_ON_ERROR(esp_timer_create(&tick_timer_args, &lvgl_tick_timer), TAG, "Create LVGL tick timer failed");
  ESP_RETURN_ON_ERROR(esp_timer_start_periodic(lvgl_tick_timer, kLvglTickPeriodMs * 1000), TAG, "Start LVGL tick timer failed");

  lv_display = lv_display_create(PANEL_WIDTH, PANEL_HEIGHT);
  ESP_RETURN_ON_FALSE(lv_display != nullptr, ESP_ERR_NO_MEM, TAG, "Create LVGL display failed");
  lv_display_set_user_data(lv_display, panel_handle);
  lv_display_set_flush_cb(lv_display, lvgl_flush);
  lv_display_set_color_format(lv_display, LV_COLOR_FORMAT_RGB565);
  lv_display_set_antialiasing(lv_display, false);

  const uint32_t full_buffer_bytes = static_cast<uint32_t>(PANEL_WIDTH) * PANEL_HEIGHT * 2U;
  const uint32_t partial_buffer_bytes = static_cast<uint32_t>(PANEL_WIDTH) * LVGL_BUFFER_LINES * 2U;

#if LVGL_USE_RGB_DOUBLE_FB
  void *fb0 = nullptr;
  void *fb1 = nullptr;
  ESP_RETURN_ON_ERROR(esp_lcd_rgb_panel_get_frame_buffer(panel_handle, 2, &fb0, &fb1), TAG, "Get RGB frame buffers failed");
  lv_display_set_buffers(lv_display, fb0, fb1, full_buffer_bytes, LV_DISPLAY_RENDER_MODE_DIRECT);
  ESP_LOGI(TAG, "LVGL9 RGB direct double FB %ux%u", PANEL_WIDTH, PANEL_HEIGHT);
#if PANEL_LVGL_ROTATION_180
  lv_display_set_rotation(lv_display, LV_DISPLAY_ROTATION_180);
  lv_display_set_matrix_rotation(lv_display, true);
  ESP_LOGI(TAG, "LVGL display rotation 180 (matrix)");
#endif
#else
  void *buf1 = heap_caps_malloc(partial_buffer_bytes, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT);
  void *buf2 = heap_caps_malloc(partial_buffer_bytes, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT);
  ESP_RETURN_ON_FALSE(buf1 != nullptr && buf2 != nullptr, ESP_ERR_NO_MEM, TAG, "Allocate LVGL buffers failed");
  lv_display_set_buffers(lv_display, buf1, buf2, partial_buffer_bytes, LV_DISPLAY_RENDER_MODE_PARTIAL);
  ESP_LOGI(TAG, "LVGL9 partial buffers");
#if PANEL_LVGL_ROTATION_180
  lv_display_set_rotation(lv_display, LV_DISPLAY_ROTATION_180);
  lv_display_set_matrix_rotation(lv_display, true);
  ESP_LOGI(TAG, "LVGL display rotation 180 (matrix)");
#endif
#endif

  return ESP_OK;
}

void display_start_lvgl_task() {
  xTaskCreatePinnedToCore(lvgl_task, "lvgl", kLvglTaskStack, nullptr, kLvglTaskPriority, nullptr, 0);
}

void display_lock() {
  if (lvgl_mutex != nullptr) {
    xSemaphoreTakeRecursive(lvgl_mutex, portMAX_DELAY);
  }
}

void display_unlock() {
  if (lvgl_mutex != nullptr) {
    xSemaphoreGiveRecursive(lvgl_mutex);
  }
}

lv_display_t *display_get() {
  return lv_display;
}
