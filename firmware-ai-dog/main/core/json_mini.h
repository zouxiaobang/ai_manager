#ifndef KYLE_CORE_JSON_MINI_H
#define KYLE_CORE_JSON_MINI_H

// 极简 JSON DOM：core/ 层禁止依赖 cJSON / ESP-IDF，这里自给一个够用的解析器。
// 覆盖对象/字符串/数字/布尔/null/数组，支持嵌套；\uXXXX 仅处理 BMP。

#include <string>
#include <utility>
#include <vector>

namespace kyle {
namespace json {

class Value {
public:
    enum class Type { kNull, kBool, kNumber, kString, kObject, kArray };

    Type type() const { return type_; }
    bool IsObject() const { return type_ == Type::kObject; }
    bool IsArray() const { return type_ == Type::kArray; }
    bool IsString() const { return type_ == Type::kString; }
    bool IsNumber() const { return type_ == Type::kNumber; }
    bool IsBool() const { return type_ == Type::kBool; }
    bool IsNull() const { return type_ == Type::kNull; }

    const Value* Get(const char* key) const {
        if (type_ != Type::kObject || key == nullptr) return nullptr;
        for (const auto& kv : members_) {
            if (kv.first == key) return &kv.second;
        }
        return nullptr;
    }

    const char* AsString(const char* def = nullptr) const {
        return type_ == Type::kString ? string_.c_str() : def;
    }
    double AsNumber(double def = 0) const {
        return type_ == Type::kNumber ? number_ : def;
    }
    bool AsBool(bool def = false) const {
        return type_ == Type::kBool ? bool_ : def;
    }

    // 对象成员（key, value）或数组元素（key 为空）
    const std::vector<std::pair<std::string, Value>>& members() const { return members_; }

    // 解析器直接写内部状态（内部工具，不对外封装）
    Type type_ = Type::kNull;
    bool bool_ = false;
    double number_ = 0;
    std::string string_;
    std::vector<std::pair<std::string, Value>> members_;
};

// 解析整段 JSON；成功且消费完整个输入返回 true
bool Parse(const char* text, Value* out);

}  // namespace json
}  // namespace kyle

#endif  // KYLE_CORE_JSON_MINI_H
