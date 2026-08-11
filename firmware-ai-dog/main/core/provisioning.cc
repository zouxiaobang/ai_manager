#include "core/provisioning.h"

namespace kyle {

std::string BuildApSsid(const std::string& mac) {
    // 去掉冒号取紧凑 MAC，再取后 4 位（热点名短且唯一标识设备）
    std::string compact;
    compact.reserve(mac.size());
    for (char c : mac) {
        if (c != ':') {
            compact.push_back(c);
        }
    }
    std::string tail;
    if (compact.size() >= 4) {
        tail = compact.substr(compact.size() - 4);
    } else {
        tail = compact;  // MAC 异常（过短）：用全部
    }
    if (tail.empty()) {
        tail = "dog";  // 完全无 MAC（NoNetwork）：回退固定后缀，避免热点只剩前缀
    }
    return std::string("kyle-") + tail;
}

std::string UrlDecode(const std::string& s) {
    std::string out;
    out.reserve(s.size());
    for (size_t i = 0; i < s.size(); ++i) {
        const char c = s[i];
        if (c == '+') {
            out.push_back(' ');  // form 编码：+ 表示空格
        } else if (c == '%' && i + 2 < s.size()) {
            // 两个十六进制位 → 一个字节（UTF-8 中文 SSID 的 %XX 序列原样还原）
            const auto hex = [](char h) -> int {
                if (h >= '0' && h <= '9') return h - '0';
                if (h >= 'a' && h <= 'f') return h - 'a' + 10;
                if (h >= 'A' && h <= 'F') return h - 'A' + 10;
                return -1;
            };
            const int hi = hex(s[i + 1]);
            const int lo = hex(s[i + 2]);
            if (hi >= 0 && lo >= 0) {
                out.push_back(static_cast<char>((hi << 4) | lo));
                i += 2;
            } else {
                out.push_back(c);  // 非法转义：原样保留
            }
        } else {
            out.push_back(c);
        }
    }
    return out;
}

ProvisionResult ParseProvisionForm(const std::string& body) {
    ProvisionResult r;
    if (body.empty()) {
        return r;
    }
    // 按 & 拆成 key=value 对；取第一个出现的 ssid/password
    size_t pos = 0;
    while (pos <= body.size()) {
        size_t amp = body.find('&', pos);
        if (amp == std::string::npos) {
            amp = body.size();
        }
        const std::string pair = body.substr(pos, amp - pos);
        const size_t eq = pair.find('=');
        std::string key = (eq == std::string::npos) ? pair : pair.substr(0, eq);
        std::string value = (eq == std::string::npos) ? "" : pair.substr(eq + 1);
        key = UrlDecode(key);
        value = UrlDecode(value);
        if (key == "ssid" && r.ssid.empty()) {
            r.ssid = value;
        } else if (key == "password" && r.password.empty()) {
            r.password = value;
        }
        if (amp == body.size()) {
            break;
        }
        pos = amp + 1;
    }
    r.ok = !r.ssid.empty();
    return r;
}

std::string BuildProvisionPage(const std::string& ap_ssid, const std::string& error) {
    // 内联 CSS、无外网资源：配网时设备无法联网，引用 CDN 会白屏。
    std::string page;
    page.reserve(1200);
    page +=
        "<!doctype html><html><head><meta charset=utf-8>"
        "<meta name=viewport content='width=device-width,initial-scale=1'>"
        "<title>Kyle WiFi 配置</title>"
        "<style>"
        "body{font-family:-apple-system,sans-serif;max-width:420px;margin:0 auto;"
        "padding:32px 20px;color:#222}"
        "h1{font-size:22px;margin-bottom:4px}"
        ".tip{color:#666;font-size:14px;line-height:1.6;margin-bottom:24px}"
        "label{display:block;font-size:14px;margin:14px 0 6px}"
        "input{width:100%;box-sizing:border-box;padding:12px;font-size:16px;"
        "border:1px solid #ccc;border-radius:8px}"
        "button{width:100%;margin-top:24px;padding:14px;font-size:16px;color:#fff;"
        "background:#1a73e8;border:0;border-radius:8px}"
        ".err{color:#c5221f;background:#fce8e6;padding:10px;border-radius:8px;"
        "margin-bottom:16px;font-size:14px}"
        "</style></head><body>";
    page += "<h1>Kyle 配网</h1>";
    page += "<p class=tip>设备热点：<b>" + ap_ssid + "</b><br>输入家里的 WiFi 名称和密码，提交后设备将自动重启并连接。</p>";
    if (!error.empty()) {
        page += "<div class=err>" + error + "</div>";
    }
    page +=
        "<form method=\"post\" action=\"/save\">"
        "<label for=\"ssid\">WiFi 名称</label>"
        "<input id=\"ssid\" name=\"ssid\" required autocomplete=\"off\">"
        "<label for=\"password\">WiFi 密码</label>"
        "<input id=\"password\" name=\"password\" type=\"password\">"
        "<button type=\"submit\">保存并连接</button>"
        "</form></body></html>";
    return page;
}

}  // namespace kyle
