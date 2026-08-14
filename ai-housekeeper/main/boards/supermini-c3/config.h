#ifndef _BOARD_CONFIG_H_
#define _BOARD_CONFIG_H_

#include <driver/gpio.h>

#define I2S_PORT I2S_NUM_0
// 音频配置 - I2S直接输入输出（无外部编解码器）
#define AUDIO_INPUT_SAMPLE_RATE  16000
#define AUDIO_OUTPUT_SAMPLE_RATE 16000

// I2S引脚配置
#define AUDIO_I2S_GPIO_MCLK      GPIO_NUM_0   // 主时钟（可选）
#define AUDIO_I2S_GPIO_WS        GPIO_NUM_5   // 字选择（WS/LRC）
#define AUDIO_I2S_GPIO_BCLK      GPIO_NUM_6   // 位时钟（BCLK）
#define AUDIO_I2S_GPIO_DIN       GPIO_NUM_4   // 麦克风数据输入（SD）
#define AUDIO_I2S_GPIO_DOUT      GPIO_NUM_7   // 功放数据输出（DIN）

#define AUDIO_I2S_MIC_GPIO_WS   GPIO_NUM_5
#define AUDIO_I2S_MIC_GPIO_SCK  GPIO_NUM_6
#define AUDIO_I2S_MIC_GPIO_DIN  GPIO_NUM_4
#define AUDIO_I2S_SPK_GPIO_DOUT GPIO_NUM_7
#define AUDIO_I2S_SPK_GPIO_BCLK GPIO_NUM_6
#define AUDIO_I2S_SPK_GPIO_LRCK GPIO_NUM_5

// 功放控制引脚（MAX98357A）
#define AUDIO_PA_ENABLE_PIN      -1   // 功放使能（可选，如果接SD到3.3V则不需要）

// 按钮配置（ESP32-C3-SuperMini板载按钮）
#define BOOT_BUTTON_GPIO         GPIO_NUM_2   // BOOT按钮
#define VOLUME_UP_BUTTON_GPIO         GPIO_NUM_1   // 音量UP按钮
#define VOLUME_DOWN_BUTTON_GPIO         GPIO_NUM_3   // 音量DOWN按钮
// LED配置
#define STATUS_LED_PIN           GPIO_NUM_12

// 显示屏配置（ST7789 1.54" IPS 240x240）
#define DISPLAY_SDA_PIN GPIO_NUM_8
#define DISPLAY_SCL_PIN GPIO_NUM_9
#define DISPLAY_WIDTH   128
#define DISPLAY_HEIGHT  64
#define DISPLAY_MIRROR_X true
#define DISPLAY_MIRROR_Y true

#endif // _BOARD_CONFIG_H_