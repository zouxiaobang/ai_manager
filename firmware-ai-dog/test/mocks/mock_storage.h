#ifndef KYLE_TEST_MOCKS_MOCK_STORAGE_H
#define KYLE_TEST_MOCKS_MOCK_STORAGE_H

#include <map>
#include <string>

#include "core/net_config.h"

// 内存版 IStorage：模拟 NVS namespace/key，供 net_config host 单测使用。
class MockStorage : public kyle::IStorage {
public:
    std::string GetString(const std::string& ns, const std::string& key,
                          const std::string& def = "") override {
        auto it = strings_.find(Key(ns, key));
        return it == strings_.end() ? def : it->second;
    }

    int GetInt(const std::string& ns, const std::string& key, int def = 0) override {
        auto it = ints_.find(Key(ns, key));
        return it == ints_.end() ? def : it->second;
    }

    void SetString(const std::string& ns, const std::string& key,
                   const std::string& value) override {
        strings_[Key(ns, key)] = value;
    }

    void SetInt(const std::string& ns, const std::string& key, int value) override {
        ints_[Key(ns, key)] = value;
    }

private:
    static std::string Key(const std::string& ns, const std::string& key) {
        return ns + "." + key;
    }

    std::map<std::string, std::string> strings_;
    std::map<std::string, int> ints_;
};

#endif  // KYLE_TEST_MOCKS_MOCK_STORAGE_H
