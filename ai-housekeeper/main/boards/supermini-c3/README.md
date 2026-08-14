# ESP32-C3 SuperMini 开发板配置

## 硬件规格
- **主控芯片**: ESP32-C3
- **Flash大小**: 4MB
- **外设配置**:
  - INMP441 麦克风 (I2S输入)
  - MAX98357A 功放 (I2S输出)
  - ST7789 1.54" IPS显示屏 (240x240, SPI接口)

## 引脚映射

| 外设 | 引脚 | ESP32-C3 GPIO |
|------|------|----------------|
| INMP441 麦克风 | SD   | GPIO4 |
|                 | WS   | GPIO5 |
|                 | SCK  | GPIO6 |
|                 | L/R  | GND |
| MAX98357A 功放 | DIN  | GPIO7 |
|                | BCLK | GPIO6（共用）|
|                | LRC  | GPIO5（共用）|
|                | GAIN | GND |
|                | SD   | 3.3V（常开）|
|                | VIN  | 5V |
| OLED 显示屏 | SCL | GPIO20 |
|               | SDA | GPIO21 |
|               | DC  | GPIO10 |
|               | RES | GPIO2 |
|               | CS  | GND |
|               | BL  | 3.3V |

## 编译说明

1. **设置目标芯片**:
   ```bash
   idf.py set-target esp32c3