#pragma once

// Waveshare ESP32-S3-Touch-LCD-7 (800x480 RGB + GT911)
#define PANEL_WIDTH 800
#define PANEL_HEIGHT 480

#define PANEL_MIRROR_X 1
#define PANEL_MIRROR_Y 1
#define TOUCH_MIRROR_X 0
#define TOUCH_MIRROR_Y 0
#define TOUCH_SWAP_XY 0

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

#define LCD_DISP_EXIO 2
#define LCD_RST_EXIO 3
#define TP_IRQ 4
#define TP_SDA 8
#define TP_SCL 9
#define TP_RST_EXIO 1
#define USB_SEL_EXIO 5
#define GT911_I2C_ADDRESS 0

/* TF card (SPI, CS on CH422G EXIO4) */
#define SD_MOSI_GPIO 11
#define SD_MISO_GPIO 13
#define SD_SCLK_GPIO 12
#define SD_CS_EXIO 4
#define SD_MOUNT_POINT "/sdcard"

#define LCD_PIXEL_CLOCK_HZ (12 * 1000 * 1000)
#define HSYNC_FRONT_PORCH 8
#define HSYNC_PULSE_WIDTH 4
#define HSYNC_BACK_PORCH 8
#define VSYNC_FRONT_PORCH 8
#define VSYNC_PULSE_WIDTH 4
#define VSYNC_BACK_PORCH 8
#define PCLK_ACTIVE_NEG 1
#define RGB_BOUNCE_BUFFER_SIZE_PX (PANEL_WIDTH * 20)
#define LVGL_BUFFER_LINES 80
