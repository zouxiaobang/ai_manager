#pragma once
#include <driver/gpio.h>
#include <driver/temperature_sensor.h>
#include <esp_adc/adc_oneshot.h>
#include <esp_log.h>
#include <esp_timer.h>
#include <math.h>
#include <functional>
#include <vector>

class PowerManager {
private:
    static constexpr const char* TAG = "KylePowerManager";

    esp_timer_handle_t timer_handle_ = nullptr;

    std::function<void(bool)> on_charging_status_changed_;
    std::function<void(bool)> on_low_battery_status_changed_;
    std::function<void(float)> on_temperature_changed_;
    std::vector<uint16_t> adc_values_;

    gpio_num_t charging_pin_ = GPIO_NUM_NC;

    // ===== 电池相关 =====
    static constexpr int kAdcSampleCount = 8;       // 滤波数量
    static constexpr int kBatteryAdcInterval = 60;  // 秒
    static constexpr int kLowBatteryLevel = 15;     // %
    const int kBatteryAdcDataCount = 3;

    uint16_t adc_buffer_[kAdcSampleCount] = {0};
    int adc_index_ = 0;
    int adc_count_ = 0;

    uint8_t battery_level_ = 0;
    bool is_charging_ = false;
    bool is_low_battery_ = false;

    // ===== 温度相关 =====
    static constexpr int kTemperatureInterval = 10;  // 秒
    float current_temperature_ = 0.0f;

    // ===== 计数 =====
    int ticks_ = 0;

    adc_oneshot_unit_handle_t adc_handle_ = nullptr;
    temperature_sensor_handle_t temp_sensor_ = nullptr;

private:
    // ================== 主循环 ==================
    void CheckStatus() {
        ticks_ = (ticks_ + 1) % 3600;  // 防溢出

        CheckCharging();

        // 如果电池电量数据不足，则读取电池电量数据
        if (adc_values_.size() < kBatteryAdcDataCount) {
            ReadBattery();
            return;
        }
        if (ticks_ % kBatteryAdcInterval == 0 || ticks_ == 10) {
            ReadBattery();
        }

        if (ticks_ % kTemperatureInterval == 0) {
            ReadTemperature();
        }
    }

    // ================== 充电检测 ==================
    void CheckCharging() {
        if (charging_pin_ == GPIO_NUM_NC) {
            return;  // 不检测充电状态
        }
        // ⚠️ 根据电路决定高低电平
        bool new_status = gpio_get_level(charging_pin_) == 0;

        if (new_status != is_charging_) {
            is_charging_ = new_status;

            ESP_LOGI(TAG, "Charging: %s", is_charging_ ? "YES" : "NO");

            if (on_charging_status_changed_) {
                on_charging_status_changed_(is_charging_);
            }
        }
    }

    // ================== ADC读取 ==================
    void ReadBattery() {
        int adc_raw;
        // 读取原始值
        if (adc_oneshot_read(adc_handle_, ADC_CHANNEL_7, &adc_raw) != ESP_OK)
            return;

        // 1. 均值滤波 (使用你之前定义的数组)
        adc_values_.push_back(adc_raw);
        if (adc_values_.size() > 10)
            adc_values_.erase(adc_values_.begin());

        uint32_t sum = 0;
        for (auto v : adc_values_)
            sum += v;
        uint32_t avg_adc = sum / adc_values_.size();

        // 2. 计算实际电压 (mV)
        // 0.862f 是单位换算，2.0f 是你的 1k+1k 分压系数
        float battery_mv = avg_adc * 0.862f * 2.0f;

        // 3. 映射电量百分比
        // 根据 3.7V 锂电池特性：4.1V 以上算满电，3.4V 以下算没电
        if (battery_mv >= 4100) {
            battery_level_ = 100;
        } else if (battery_mv <= 3400) {
            battery_level_ = 0;
        } else {
            // 使用线性公式换算 (4100-3400 = 700mV 区间)
            battery_level_ = (uint8_t)((battery_mv - 3400) / 700.0f * 100);
        }
    }

    // ================== ADC转电量 ==================
    uint8_t AdcToPercent(uint32_t adc) {
        struct Level {
            uint16_t adc;
            uint8_t percent;
        };

        // 更接近真实锂电池曲线
        static const Level table[] = {{2050, 0},  {2100, 5},  {2150, 10}, {2200, 20},
                                      {2250, 30}, {2300, 40}, {2350, 50}, {2400, 60},
                                      {2450, 70}, {2500, 80}, {2550, 90}, {2600, 100}};
        float adc_voltage = (float)adc / 4095.0f * 3.3f;

        // 因为分压了2倍
        float battery_voltage = adc_voltage * 2.0f;

        if (adc <= table[0].adc)
            return 0;
        if (adc >= table[11].adc)
            return 100;

        for (int i = 0; i < 11; i++) {
            if (adc >= table[i].adc && adc < table[i + 1].adc) {
                float ratio = (float)(adc - table[i].adc) / (table[i + 1].adc - table[i].adc);

                return table[i].percent + ratio * (table[i + 1].percent - table[i].percent);
            }
        }

        return 0;
    }

    // ================== 温度 ==================
    void ReadTemperature() {
        float temp = 0.0f;
        ESP_ERROR_CHECK(temperature_sensor_get_celsius(temp_sensor_, &temp));

        if (fabsf(temp - current_temperature_) >= 1.0f) {
            current_temperature_ = temp;

            ESP_LOGI(TAG, "Temp: %.2f°C", temp);

            if (on_temperature_changed_) {
                on_temperature_changed_(temp);
            }
        }
    }

public:
    PowerManager(gpio_num_t pin) : charging_pin_(pin) {
        // ===== GPIO =====
        if (charging_pin_ != GPIO_NUM_NC) {
            gpio_config_t io_conf = {};
            io_conf.mode = GPIO_MODE_INPUT;
            io_conf.pin_bit_mask = (1ULL << charging_pin_);
            gpio_config(&io_conf);
        }

        // ===== 定时器 =====
        esp_timer_create_args_t timer_args = {
            .callback = [](void* arg) { static_cast<PowerManager*>(arg)->CheckStatus(); },
            .arg = this,
            .dispatch_method = ESP_TIMER_TASK,
            .name = "power_timer"};

        ESP_ERROR_CHECK(esp_timer_create(&timer_args, &timer_handle_));
        ESP_ERROR_CHECK(esp_timer_start_periodic(timer_handle_, 1000000));

        // ===== ADC =====
        adc_oneshot_unit_init_cfg_t init_config = {
            .unit_id = ADC_UNIT_1,
            .ulp_mode = ADC_ULP_MODE_DISABLE,
        };
        ESP_ERROR_CHECK(adc_oneshot_new_unit(&init_config, &adc_handle_));

        adc_oneshot_chan_cfg_t chan_config = {
            .atten = ADC_ATTEN_DB_12,
            .bitwidth = ADC_BITWIDTH_12,
        };
        ESP_ERROR_CHECK(adc_oneshot_config_channel(adc_handle_, ADC_CHANNEL_7, &chan_config));

        // ===== 温度 =====
        temperature_sensor_config_t temp_config = {
            .range_min = 10,
            .range_max = 80,
        };
        ESP_ERROR_CHECK(temperature_sensor_install(&temp_config, &temp_sensor_));
        ESP_ERROR_CHECK(temperature_sensor_enable(temp_sensor_));

        for (int i = 0; i < kAdcSampleCount; i++) {
            // 假设 CheckStatus 执行一次采样
            CheckStatus();
        }
    }

    ~PowerManager() {
        if (timer_handle_) {
            esp_timer_stop(timer_handle_);
            esp_timer_delete(timer_handle_);
        }

        if (adc_handle_) {
            adc_oneshot_del_unit(adc_handle_);
        }

        if (temp_sensor_) {
            temperature_sensor_disable(temp_sensor_);
            temperature_sensor_uninstall(temp_sensor_);
        }
    }

    // ================== 对外接口 ==================

    bool IsCharging() const {
        if (charging_pin_ == GPIO_NUM_NC) {
            return false;
        }
        return (battery_level_ < 100) && is_charging_;
    }

    bool IsDischarging() const { return !IsCharging(); }

    uint8_t GetBatteryLevel() const { return battery_level_; }

    float GetTemperature() const { return current_temperature_; }

    void OnChargingStatusChanged(std::function<void(bool)> cb) { on_charging_status_changed_ = cb; }

    void OnLowBatteryStatusChanged(std::function<void(bool)> cb) {
        on_low_battery_status_changed_ = cb;
    }

    void OnTemperatureChanged(std::function<void(float)> cb) { on_temperature_changed_ = cb; }
};