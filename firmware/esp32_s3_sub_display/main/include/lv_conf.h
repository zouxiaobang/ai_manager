/**
 * @file lv_conf.h
 * LVGL config for ESP32-S3 sub display demo.
 */
#ifndef LV_CONF_H
#define LV_CONF_H

#include "sdkconfig.h"

#define LV_COLOR_DEPTH 16
#define LV_USE_STDLIB_MALLOC LV_STDLIB_CLIB
#define LV_USE_STDLIB_STRING LV_STDLIB_CLIB
#define LV_USE_STDLIB_SPRINTF LV_STDLIB_CLIB
#define LV_DEF_REFR_PERIOD 16
#define LV_DPI_DEF 130
#define LV_USE_OS LV_OS_NONE

#define LV_DRAW_SW_DRAW_UNIT_CNT 1
#define LV_USE_DRAW_SW 1
#define LV_USE_DRAW_SW_ASM LV_DRAW_SW_ASM_NONE

#define LV_USE_LOG 0

#define LV_FONT_MONTSERRAT_14 1
#define LV_FONT_MONTSERRAT_20 1
#define LV_FONT_MONTSERRAT_28 1
#define LV_FONT_DEFAULT &lv_font_montserrat_20

#define LV_USE_LABEL 1
#define LV_USE_BUTTON 1
#define LV_USE_BAR 1
#define LV_USE_FLEX 1

/* GB2312 lyrics font (generate: fonts/generate_gb2312_font.py) */
#define LV_FONT_CN_GB2312_16_0 1
#define LV_FONT_CN_GB2312_16_1 1
#define LV_FONT_CN_GB2312_16_2 1
#define LV_FONT_CN_GB2312_16_3 1
#define LV_FONT_CN_GB2312_16_4 1
#define LV_FONT_CN_GB2312_16_5 1
#define LV_FONT_CN_GB2312_16_6 1
#define LV_FONT_CN_GB2312_16_7 1

#define LV_USE_THEME_DEFAULT 1
#define LV_THEME_DEFAULT_DARK 1

#define LV_USE_IMAGE 1
#define LV_USE_LODEPNG 1

#define LV_USE_FS_POSIX 1
#define LV_FS_POSIX_LETTER 'A'
#define LV_FS_POSIX_PATH "/sdcard"
#define LV_FS_POSIX_CACHE_SIZE 16384

#endif /* LV_CONF_H */
