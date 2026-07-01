#pragma once

#include <stddef.h>
#include <stdint.h>

#include "esp_err.h"

extern const uint8_t k_seed_assets_tomato_png[];
extern const size_t k_seed_assets_tomato_png_len;
extern const uint8_t k_seed_assets_icon_wifi_png[];
extern const size_t k_seed_assets_icon_wifi_png_len;
extern const uint8_t k_seed_assets_icon_lock_png[];
extern const size_t k_seed_assets_icon_lock_png_len;
extern const uint8_t k_seed_assets_icon_unlock_png[];
extern const size_t k_seed_assets_icon_unlock_png_len;
extern const uint8_t k_seed_assets_icon_eq_png[];
extern const size_t k_seed_assets_icon_eq_png_len;
extern const uint8_t k_seed_assets_deco_diamond_png[];
extern const size_t k_seed_assets_deco_diamond_png_len;
extern const uint8_t k_seed_assets_deco_diamond_blue_png[];
extern const size_t k_seed_assets_deco_diamond_blue_png_len;
extern const uint8_t k_seed_assets_dock_pomo_png[];
extern const size_t k_seed_assets_dock_pomo_png_len;
extern const uint8_t k_seed_assets_dock_home_png[];
extern const size_t k_seed_assets_dock_home_png_len;
extern const uint8_t k_seed_assets_dock_lyrics_png[];
extern const size_t k_seed_assets_dock_lyrics_png_len;
extern const uint8_t k_seed_assets_dock_lock_png[];
extern const size_t k_seed_assets_dock_lock_png_len;
extern const uint8_t k_seed_assets_dock_settings_png[];
extern const size_t k_seed_assets_dock_settings_png_len;
extern const uint8_t k_seed_lyrics_current_meta[];
extern const size_t k_seed_lyrics_current_meta_len;
extern const uint8_t k_seed_lyrics_current_txt[];
extern const size_t k_seed_lyrics_current_txt_len;

size_t assets_seed_expected_len(const char *relative_path);
esp_err_t assets_seed_sdcard(void);
