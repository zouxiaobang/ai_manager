#include "drivers/st7789_lcd.h"

#include <memory>

#include "driver/spi_common.h"
#include "esp_lcd_panel_io.h"
#include "esp_lcd_panel_ops.h"
#include "esp_lcd_panel_vendor.h"
#include "esp_log.h"

#include "drivers/lcd_pattern.h"

#define TAG "St7789Lcd"

namespace kyle {

// esp_lcd 句柄藏在 pimpl：头文件保持零 IDF 依赖，便于 host 测试引用 St7789Config。
struct St7789Lcd::Impl {
    esp_lcd_panel_io_handle_t panel_io = nullptr;
    esp_lcd_panel_handle_t panel = nullptr;
    bool init_ok = false;
};

St7789Lcd::St7789Lcd(const St7789Config& cfg)
    : cfg_(cfg), impl_(std::make_unique<Impl>()) {}

St7789Lcd::~St7789Lcd() = default;

void St7789Lcd::Init() {
    // SPI3_HOST，max_transfer_sz 至少容纳整帧（240*240*2），对齐旧 InitializeSpi。
    // 用 {} 全量清零再逐字段赋值，避免 designated-init 缺省字段的告警。
    spi_bus_config_t bus_cfg = {};
    bus_cfg.mosi_io_num = cfg_.mosi;
    bus_cfg.miso_io_num = GPIO_NUM_NC;
    bus_cfg.sclk_io_num = cfg_.clk;
    bus_cfg.quadwp_io_num = GPIO_NUM_NC;
    bus_cfg.quadhd_io_num = GPIO_NUM_NC;
    bus_cfg.max_transfer_sz = cfg_.width * cfg_.height * 2 + 1024;
    ESP_ERROR_CHECK(spi_bus_initialize(SPI3_HOST, &bus_cfg, SPI_DMA_CH_AUTO));

    esp_lcd_panel_io_spi_config_t io_cfg = {};
    io_cfg.cs_gpio_num = cfg_.cs;
    io_cfg.dc_gpio_num = cfg_.dc;
    io_cfg.spi_mode = 0;
    io_cfg.pclk_hz = cfg_.pclk_hz;
    io_cfg.trans_queue_depth = 10;
    io_cfg.lcd_cmd_bits = 8;
    io_cfg.lcd_param_bits = 8;
    ESP_ERROR_CHECK(esp_lcd_new_panel_io_spi(SPI3_HOST, &io_cfg, &impl_->panel_io));

    esp_lcd_panel_dev_config_t panel_cfg = {};
    panel_cfg.reset_gpio_num = cfg_.rst;
    panel_cfg.rgb_ele_order = LCD_RGB_ELEMENT_ORDER_RGB;
    panel_cfg.bits_per_pixel = 16;
    ESP_ERROR_CHECK(esp_lcd_new_panel_st7789(impl_->panel_io, &panel_cfg, &impl_->panel));

    ESP_ERROR_CHECK(esp_lcd_panel_reset(impl_->panel));
    ESP_ERROR_CHECK(esp_lcd_panel_init(impl_->panel));
    ESP_ERROR_CHECK(esp_lcd_panel_invert_color(impl_->panel, cfg_.invert));
    ESP_ERROR_CHECK(esp_lcd_panel_swap_xy(impl_->panel, cfg_.swap_xy));
    ESP_ERROR_CHECK(esp_lcd_panel_mirror(impl_->panel, cfg_.mirror_x, cfg_.mirror_y));
    // esp_lcd_panel_init 只发 SLPOUT 退出睡眠，不会发 0x29(Display On)。
    // 不开显示则 GRAM 有内容但整屏黑（旧项目 KyleV1Display 同样显式开了显示）。
    ESP_ERROR_CHECK(esp_lcd_panel_disp_on_off(impl_->panel, true));

    impl_->init_ok = true;
    ShowPattern(0);
    ESP_LOGI(TAG, "ST7789 240x240 初始化完成，显示测试图案");
}

void St7789Lcd::ShowPattern(int pattern) {
    if (!impl_->init_ok) {
        return;
    }
    const int w = cfg_.width;
    const int h = cfg_.height;
    auto buf = std::make_unique<uint16_t[]>(w * h);
    if (pattern == 0) {
        lcd_pattern::RenderColorBars(buf.get(), w, h);
    } else {
        lcd_pattern::RenderCheckerboard(buf.get(), w, h, 16);
    }
    // ST7789 RAMCTRL 默认大端，主机小端缓冲需逐像素换字节再上屏（对齐旧 LVGL swap_bytes=1）
    lcd_pattern::ByteSwap565(buf.get(), static_cast<size_t>(w) * h);
    // 全帧一次绘制，DMA 传输
    ESP_ERROR_CHECK(esp_lcd_panel_draw_bitmap(impl_->panel, 0, 0, w, h, buf.get()));
}

// K1 阶段无文本渲染（LVGL 在 F4），状态类接口暂用图案占位，避免黑屏误解。
void St7789Lcd::SetStatus(const char* s) {
    (void)s;
    ShowPattern(0);
}

void St7789Lcd::SetChatMessage(const char* role, const char* text) {
    (void)role;
    (void)text;
}

void St7789Lcd::SetEmotion(const char* e) {
    (void)e;
}

void St7789Lcd::ShowToast(const char* msg, int ms) {
    (void)msg;
    (void)ms;
}

int St7789Lcd::width() const { return cfg_.width; }
int St7789Lcd::height() const { return cfg_.height; }

}  // namespace kyle
