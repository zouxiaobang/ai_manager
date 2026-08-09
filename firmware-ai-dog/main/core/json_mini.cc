#include "core/json_mini.h"

#include <cctype>
#include <cstdlib>
#include <cstring>

namespace kyle {
namespace json {

namespace {

class Parser {
public:
    explicit Parser(const char* text) : p_(text) {}

    bool Parse(Value* out) {
        if (!ParseValue(out)) return false;
        SkipWhitespace();
        return *p_ == '\0';  // 必须消费完整段
    }

private:
    const char* p_;

    void SkipWhitespace() {
        while (*p_ == ' ' || *p_ == '\t' || *p_ == '\n' || *p_ == '\r') ++p_;
    }

    bool ParseValue(Value* out) {
        SkipWhitespace();
        if (*p_ == '{') return ParseObject(out);
        if (*p_ == '[') return ParseArray(out);
        if (*p_ == '"') return ParseString(out);
        if (*p_ == 't' || *p_ == 'f') return ParseBool(out);
        if (*p_ == 'n') { out->type_ = Value::Type::kNull; p_ += 4; return true; }
        return ParseNumber(out);
    }

    bool ParseObject(Value* out) {
        ++p_;  // '{'
        out->type_ = Value::Type::kObject;
        SkipWhitespace();
        if (*p_ == '}') { ++p_; return true; }
        for (;;) {
            SkipWhitespace();
            if (*p_ != '"') return false;
            std::string key;
            if (!ParseStringInto(&key)) return false;
            SkipWhitespace();
            if (*p_ != ':') return false;
            ++p_;
            Value v;
            if (!ParseValue(&v)) return false;
            out->members_.emplace_back(std::move(key), std::move(v));
            SkipWhitespace();
            if (*p_ == ',') { ++p_; continue; }
            if (*p_ == '}') { ++p_; return true; }
            return false;
        }
    }

    bool ParseArray(Value* out) {
        ++p_;  // '['
        out->type_ = Value::Type::kArray;
        SkipWhitespace();
        if (*p_ == ']') { ++p_; return true; }
        for (;;) {
            Value v;
            if (!ParseValue(&v)) return false;
            out->members_.emplace_back(std::string(), std::move(v));
            SkipWhitespace();
            if (*p_ == ',') { ++p_; continue; }
            if (*p_ == ']') { ++p_; return true; }
            return false;
        }
    }

    bool ParseString(Value* out) {
        std::string s;
        if (!ParseStringInto(&s)) return false;
        out->type_ = Value::Type::kString;
        out->string_ = std::move(s);
        return true;
    }

    bool ParseStringInto(std::string* out) {
        if (*p_ != '"') return false;
        ++p_;
        out->clear();
        while (*p_ != '"') {
            if (*p_ == '\0') return false;
            if (*p_ == '\\') {
                ++p_;
                switch (*p_) {
                    case '"': out->push_back('"'); break;
                    case '\\': out->push_back('\\'); break;
                    case '/': out->push_back('/'); break;
                    case 'n': out->push_back('\n'); break;
                    case 't': out->push_back('\t'); break;
                    case 'r': out->push_back('\r'); break;
                    case 'b': out->push_back('\b'); break;
                    case 'f': out->push_back('\f'); break;
                    case 'u': {
                        // 仅 BMP，不做代理对合并
                        if (p_[1] == '\0' || p_[2] == '\0' || p_[3] == '\0' || p_[4] == '\0')
                            return false;
                        unsigned code = 0;
                        for (int i = 1; i <= 4; ++i) {
                            char c = p_[i];
                            code <<= 4;
                            if (c >= '0' && c <= '9') code |= static_cast<unsigned>(c - '0');
                            else if (c >= 'a' && c <= 'f') code |= static_cast<unsigned>(c - 'a' + 10);
                            else if (c >= 'A' && c <= 'F') code |= static_cast<unsigned>(c - 'A' + 10);
                            else return false;
                        }
                        p_ += 4;
                        if (code < 0x80) {
                            out->push_back(static_cast<char>(code));
                        } else if (code < 0x800) {
                            out->push_back(static_cast<char>(0xC0 | (code >> 6)));
                            out->push_back(static_cast<char>(0x80 | (code & 0x3F)));
                        } else {
                            out->push_back(static_cast<char>(0xE0 | (code >> 12)));
                            out->push_back(static_cast<char>(0x80 | ((code >> 6) & 0x3F)));
                            out->push_back(static_cast<char>(0x80 | (code & 0x3F)));
                        }
                        break;
                    }
                    default: return false;
                }
                ++p_;
            } else {
                out->push_back(*p_);
                ++p_;
            }
        }
        ++p_;  // 收尾引号
        return true;
    }

    bool ParseBool(Value* out) {
        if (std::strncmp(p_, "true", 4) == 0) {
            out->type_ = Value::Type::kBool; out->bool_ = true; p_ += 4; return true;
        }
        if (std::strncmp(p_, "false", 5) == 0) {
            out->type_ = Value::Type::kBool; out->bool_ = false; p_ += 5; return true;
        }
        return false;
    }

    bool ParseNumber(Value* out) {
        const char* start = p_;
        if (*p_ == '-') ++p_;
        if (*p_ == '0') {
            ++p_;
        } else if (*p_ >= '1' && *p_ <= '9') {
            while (*p_ >= '0' && *p_ <= '9') ++p_;
        } else {
            return false;
        }
        if (*p_ == '.') {
            ++p_;
            if (*p_ < '0' || *p_ > '9') return false;
            while (*p_ >= '0' && *p_ <= '9') ++p_;
        }
        if (*p_ == 'e' || *p_ == 'E') {
            ++p_;
            if (*p_ == '+' || *p_ == '-') ++p_;
            if (*p_ < '0' || *p_ > '9') return false;
            while (*p_ >= '0' && *p_ <= '9') ++p_;
        }
        out->type_ = Value::Type::kNumber;
        out->number_ = std::strtod(start, nullptr);
        return true;
    }
};

}  // namespace

bool Parse(const char* text, Value* out) {
    if (text == nullptr || out == nullptr) return false;
    Parser parser(text);
    return parser.Parse(out);
}

}  // namespace json
}  // namespace kyle
