#include "app/nvs_storage.h"

#include <cstring>

#include "esp_log.h"
#include "nvs.h"
#include "nvs_flash.h"

namespace kyle {

namespace {
constexpr const char* kTag = "NvsStorage";
}

NvsStorage::NvsStorage() = default;
NvsStorage::~NvsStorage() = default;

std::string NvsStorage::GetString(const std::string& ns, const std::string& key,
                                  const std::string& def) {
    nvs_handle_t handle;
    std::string result = def;
    if (nvs_open(ns.c_str(), NVS_READONLY, &handle) == ESP_OK) {
        size_t len = 0;
        if (nvs_get_str(handle, key.c_str(), nullptr, &len) == ESP_OK && len > 0) {
            std::string buf(len, '\0');
            if (nvs_get_str(handle, key.c_str(), &buf[0], &len) == ESP_OK) {
                buf.resize(len);                       // len 含结尾 '\0'
                if (!buf.empty() && buf.back() == '\0') buf.pop_back();  // 去掉末尾 '\0'
                result = buf;
            }
        }
        nvs_close(handle);
    }
    return result;
}

int NvsStorage::GetInt(const std::string& ns, const std::string& key, int def) {
    nvs_handle_t handle;
    int32_t value = def;
    if (nvs_open(ns.c_str(), NVS_READONLY, &handle) == ESP_OK) {
        nvs_get_i32(handle, key.c_str(), &value);
        nvs_close(handle);
    }
    return static_cast<int>(value);
}

void NvsStorage::SetString(const std::string& ns, const std::string& key,
                           const std::string& value) {
    nvs_handle_t handle;
    if (nvs_open(ns.c_str(), NVS_READWRITE, &handle) != ESP_OK) {
        ESP_LOGE(kTag, "nvs_open(%s) RW failed", ns.c_str());
        return;
    }
    esp_err_t err = nvs_set_str(handle, key.c_str(), value.c_str());
    if (err != ESP_OK) ESP_LOGE(kTag, "nvs_set_str(%s/%s) failed: %d", ns.c_str(), key.c_str(), err);
    nvs_commit(handle);
    nvs_close(handle);
}

void NvsStorage::SetInt(const std::string& ns, const std::string& key, int value) {
    nvs_handle_t handle;
    if (nvs_open(ns.c_str(), NVS_READWRITE, &handle) != ESP_OK) {
        ESP_LOGE(kTag, "nvs_open(%s) RW failed", ns.c_str());
        return;
    }
    esp_err_t err = nvs_set_i32(handle, key.c_str(), static_cast<int32_t>(value));
    if (err != ESP_OK) ESP_LOGE(kTag, "nvs_set_i32(%s/%s) failed: %d", ns.c_str(), key.c_str(), err);
    nvs_commit(handle);
    nvs_close(handle);
}

}  // namespace kyle
