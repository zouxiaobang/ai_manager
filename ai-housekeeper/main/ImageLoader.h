#pragma once

#include "lvgl.h"
#include "src/widgets/canvas/lv_canvas.h"
#include "src/draw/lv_draw.h"
#include "esp_heap_caps.h"
#include "esp_log.h"
#include "assets.h"
#include <map>
#include <string>
#include <cstring>

class ImageLoader {
private:
    // 使用结构体存储，确保数据和描述符一一对应
    struct CachedImage {
        uint8_t* buffer;
        lv_image_dsc_t dsc;
    };

public:
    /**
     * @brief 预解码 PNG 图片到 PSRAM
     * @param asset_name 资源名称
     * @param w 宽度 (必须与原始图片一致)
     * @param h 高度 (必须与原始图片一致)
     */
    static lv_image_dsc_t* GetImage(const char* asset_name, int w, int h) {
        static const char* TAG = "ImageLoader";
        static std::map<std::string, CachedImage> image_cache;

        // 1. 检查缓存，防止重复解码浪费 PSRAM
        if (image_cache.find(asset_name) != image_cache.end()) {
            return &image_cache[asset_name].dsc;
        }

        // 2. 加载原始数据
        size_t size = 0;
        void* raw_ptr = nullptr;
        if (!Assets::GetInstance().GetAssetData(asset_name, raw_ptr, size)) {
            ESP_LOGE(TAG, "资源读取失败: %s", asset_name);
            return nullptr;
        }

        // 3. 申请独立的 PSRAM 空间
        uint32_t buf_size = w * h * 4; // ARGB8888
        uint8_t* decoded_buf = (uint8_t*)heap_caps_malloc(buf_size, MALLOC_CAP_SPIRAM);
        if (!decoded_buf) {
            ESP_LOGE(TAG, "PSRAM 内存不足: %s", asset_name);
            return nullptr;
        }

        // 4. 使用画布进行解码转换
        // 关键：创建一个临时的独立画布，不与当前屏幕活跃对象直接挂钩
        lv_obj_t* canvas = lv_canvas_create(nullptr); // 不直接挂载到屏幕
        lv_canvas_set_buffer(canvas, decoded_buf, w, h, LV_COLOR_FORMAT_ARGB8888);
        lv_canvas_fill_bg(canvas, lv_color_hex(0x000000), LV_OPA_TRANSP);

        lv_image_dsc_t raw_png_dsc;
        std::memset(&raw_png_dsc, 0, sizeof(lv_image_dsc_t));
        raw_png_dsc.header.cf = LV_COLOR_FORMAT_RAW;
        raw_png_dsc.header.w = w;
        raw_png_dsc.header.h = h;
        raw_png_dsc.header.magic = LV_IMAGE_HEADER_MAGIC;
        raw_png_dsc.data_size = size;
        raw_png_dsc.data = (const uint8_t*)raw_ptr;

        // LVGL 9 绘图上下文初始化
        lv_layer_t layer;
        lv_canvas_init_layer(canvas, &layer);
        
        lv_draw_image_dsc_t draw_dsc;
        lv_draw_image_dsc_init(&draw_dsc);
        draw_dsc.src = &raw_png_dsc;

        lv_area_t coords = {0, 0, (int16_t)(w - 1), (int16_t)(h - 1)};
        lv_draw_image(&layer, &draw_dsc, &coords);

        lv_canvas_finish_layer(canvas, &layer);
        lv_obj_delete(canvas); // 彻底销毁临时对象

        // 5. 存储到缓存
        CachedImage cached;
        cached.buffer = decoded_buf;
        std::memset(&cached.dsc, 0, sizeof(lv_image_dsc_t));
        cached.dsc.header.cf = LV_COLOR_FORMAT_ARGB8888;
        cached.dsc.header.w = w;
        cached.dsc.header.h = h;
        cached.dsc.header.magic = LV_IMAGE_HEADER_MAGIC;
        cached.dsc.data = decoded_buf;
        cached.dsc.data_size = buf_size;

        image_cache[asset_name] = cached;
        ESP_LOGI(TAG, "图片解码成功并存入缓存: %s", asset_name);
        
        return &image_cache[asset_name].dsc;
    }
};