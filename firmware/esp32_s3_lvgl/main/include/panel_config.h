#pragma once

// 7-inch 800x480 RGB IPS touch panel pin map from the display manual.
#define PANEL_WIDTH 800
#define PANEL_HEIGHT 480

// 1=启动后只显示测量用图形（量完屏幕比例后改回 0 再编译）
#define PANEL_CALIBRATION_ENABLED 0

// 实测 ESP32-S3-Touch-LCD-7：400px 方框 77×70mm，280px 方框 54×50mm（仍略扁故再收一点宽度）
#define PANEL_ASPECT_W_NUM 77
#define PANEL_ASPECT_W_DENOM 77
#define PANEL_ASPECT_H_NUM 77
#define PANEL_ASPECT_H_DENOM 77
#define PANEL_VISUAL_W(nominal_px) (((nominal_px) * PANEL_ASPECT_W_NUM + (PANEL_ASPECT_W_DENOM) / 2) / PANEL_ASPECT_W_DENOM)
#define PANEL_VISUAL_H(nominal_px) (((nominal_px) * PANEL_ASPECT_H_NUM + (PANEL_ASPECT_H_DENOM) / 2) / PANEL_ASPECT_H_DENOM)

// 0=局部缓冲+draw_bitmap（硬件 mirror 校正方向，已验证不倒立）
// 1=双缓冲直连（勿开 180° 旋转，否则会倒立）
#define LVGL_USE_RGB_DOUBLE_FB 0

#define PANEL_LVGL_ROTATION_180 0

#define PANEL_MIRROR_X 1
#define PANEL_MIRROR_Y 1
#define TOUCH_MIRROR_X 1
#define TOUCH_MIRROR_Y 1

// LCD interface. Names match the display manual.
#define LCD_G3 0
#define LCD_R3 1
#define LCD_R4 2
#define LCD_VSYNC 3
#define LCD_DE 5
#define LCD_PCLK 7
#define LCD_B7 10
#define LCD_B3 14
#define LCD_B6 17
#define LCD_B5 18
#define LCD_G7 21
#define LCD_B4 38
#define LCD_G2 39
#define LCD_R7 40
#define LCD_R6 41
#define LCD_R5 42
#define LCD_G4 45
#define LCD_HSYNC 46
#define LCD_G6 47
#define LCD_G5 48

// LCD DISP is on CH422G EXIO2, not a direct ESP32-S3 GPIO.
#define LCD_DISP_EXIO 2

// Touch interface. Names match the display manual.
#define TP_IRQ 4
#define TP_SDA 8
#define TP_SCL 9

// Set to 0 to auto-detect GT911 address. GT911 common addresses: 0x5D and 0x14.
#define GT911_I2C_ADDRESS 0

// Disable touch overlay for normal use to avoid unnecessary redraw/flicker.
#define TOUCH_TEST_ENABLED 0
// GT911 is polled in one task, then LVGL reads the cached point from indev.
// This avoids consuming GT911 data twice while still using LVGL button events.
#define TOUCH_POLL_TASK_ENABLED 1
#define TOUCH_SWAP_XY 0
#define TOUCH_INVERT_Y 0

// Touch reset is on CH422G EXIO1, not a direct ESP32-S3 GPIO.
#define TP_RST_EXIO 1

// RGB panel timings. Tune these values according to the panel datasheet.
// Vendor ESP-IDF demo uses 12 MHz for this panel.
#define LCD_PIXEL_CLOCK_HZ (12 * 1000 * 1000)
#define HSYNC_FRONT_PORCH 8
#define HSYNC_PULSE_WIDTH 4
#define HSYNC_BACK_PORCH 8
#define VSYNC_FRONT_PORCH 8
#define VSYNC_PULSE_WIDTH 4
#define VSYNC_BACK_PORCH 8
#define PCLK_ACTIVE_NEG 1

// ESP32-S3 RGB LCD needs a bounce buffer to avoid horizontal screen drift.
// This follows the vendor demo value: panel width * 10.
#define RGB_BOUNCE_BUFFER_SIZE_PX (PANEL_WIDTH * 10)

#define LVGL_BUFFER_LINES 100

#define WIFI_SSID "kyle2.4"
#define WIFI_PASSWORD "Asd123456"
#define PC_DAEMON_HOST "192.168.0.119"
#define PC_DAEMON_PORT 8765
#define WS_PATH "/ws"

// 管理后台 API（番茄钟直连，与 PC_DAEMON_HOST 通常为同一台电脑局域网 IP）
#define ADMIN_API_HOST "192.168.0.119"
#define ADMIN_API_PORT 8080
#define ADMIN_API_SESSION_PATH "/api/pomodoro/session"
#define ADMIN_API_PLAN_DEFAULT_PATH "/api/pomodoro/plans/default"
#define ADMIN_API_RECORDS_PATH "/api/pomodoro/records"
#define ADMIN_API_STATS_TODAY_PATH "/api/pomodoro/stats/today"
