#ifndef XIAOZHI_APP_NVS_STORAGE_H
#define XIAOZHI_APP_NVS_STORAGE_H

#include <string>

#include "core/net_config.h"

namespace xiaozhi {

// NVS 存储适配：实现 IStorage，键存于对应 namespace（与旧项目 NVS 布局兼容：
// "websocket"/"mqtt"/"wifi" 等 namespace 不动）。
class NvsStorage : public IStorage {
public:
    NvsStorage();
    ~NvsStorage() override;

    std::string GetString(const std::string& ns, const std::string& key,
                          const std::string& def = "") override;
    int GetInt(const std::string& ns, const std::string& key, int def = 0) override;
    void SetString(const std::string& ns, const std::string& key,
                   const std::string& value) override;
    void SetInt(const std::string& ns, const std::string& key, int value) override;

private:
    // 读写句柄按需打开；持有打开状态以减少开销
    bool writable_ = false;
};

}  // namespace xiaozhi

#endif  // XIAOZHI_APP_NVS_STORAGE_H
