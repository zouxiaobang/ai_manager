#include "sd_storage.h"

#include "board_io.h"
#include "esp_check.h"
#include "esp_log.h"
#include "esp_vfs_fat.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "panel_config.h"
#include "sdmmc_cmd.h"

#include <driver/gpio.h>
#include <driver/sdspi_host.h>
#include <driver/spi_common.h>

namespace {
constexpr char TAG[] = "sd_storage";
bool s_mounted = false;
sdmmc_card_t *s_card = nullptr;

/** Toggle CS high and clock the bus so the card can power up (CS high). */
esp_err_t sd_spi_idle_clocks(spi_host_device_t host_id) {
  spi_device_handle_t spi = nullptr;
  spi_device_interface_config_t dev_cfg = {};
  dev_cfg.clock_speed_hz = 400 * 1000;
  dev_cfg.mode = 0;
  dev_cfg.spics_io_num = GPIO_NUM_NC;
  dev_cfg.queue_size = 1;

  esp_err_t err = spi_bus_add_device(host_id, &dev_cfg, &spi);
  if (err != ESP_OK) {
    return err;
  }

  uint8_t tx[16] = {};
  uint8_t rx[16] = {};
  spi_transaction_t trans = {};
  trans.length = sizeof(tx) * 8;
  trans.tx_buffer = tx;
  trans.rx_buffer = rx;
  err = spi_device_transmit(spi, &trans);
  spi_bus_remove_device(spi);
  return err;
}
}  // namespace

esp_err_t sd_storage_init() {
  if (s_mounted) {
    return ESP_OK;
  }

  ESP_RETURN_ON_ERROR(board_sd_cs_set(false), TAG, "SD CS idle failed");
  vTaskDelay(pdMS_TO_TICKS(10));

  spi_bus_config_t bus_cfg = {};
  bus_cfg.mosi_io_num = SD_MOSI_GPIO;
  bus_cfg.miso_io_num = SD_MISO_GPIO;
  bus_cfg.sclk_io_num = SD_SCLK_GPIO;
  bus_cfg.quadwp_io_num = GPIO_NUM_NC;
  bus_cfg.quadhd_io_num = GPIO_NUM_NC;
  bus_cfg.max_transfer_sz = 4000;

  esp_err_t err = spi_bus_initialize(SPI2_HOST, &bus_cfg, SDSPI_DEFAULT_DMA);
  if (err != ESP_OK && err != ESP_ERR_INVALID_STATE) {
    ESP_LOGE(TAG, "SPI bus init failed: %s", esp_err_to_name(err));
    return err;
  }

  err = sd_spi_idle_clocks(SPI2_HOST);
  if (err != ESP_OK) {
    ESP_LOGW(TAG, "SPI idle clocks skipped: %s", esp_err_to_name(err));
  }

  sdmmc_host_t host = SDSPI_HOST_DEFAULT();
  host.max_freq_khz = SDMMC_FREQ_PROBING;
  host.command_timeout_ms = 10000;

  sdspi_device_config_t slot_config = SDSPI_DEVICE_CONFIG_DEFAULT();
  slot_config.host_id = static_cast<spi_host_device_t>(host.slot);
  slot_config.gpio_cs = GPIO_NUM_NC;

  esp_vfs_fat_sdmmc_mount_config_t mount_config = {};
  mount_config.format_if_mount_failed = false;
  mount_config.max_files = 6;
  mount_config.allocation_unit_size = 16 * 1024;

  ESP_RETURN_ON_ERROR(board_sd_cs_set(true), TAG, "SD CS assert failed");
  vTaskDelay(pdMS_TO_TICKS(20));

  err = esp_vfs_fat_sdspi_mount(SD_MOUNT_POINT, &host, &slot_config, &mount_config, &s_card);
  if (err != ESP_OK) {
    ESP_LOGE(TAG, "SD mount failed: %s", esp_err_to_name(err));
    board_sd_cs_set(false);
    return err;
  }

  s_mounted = true;
  ESP_LOGI(TAG, "SD mounted at %s", SD_MOUNT_POINT);
  sdmmc_card_print_info(stdout, s_card);
  return ESP_OK;
}

bool sd_storage_is_mounted() {
  return s_mounted;
}
