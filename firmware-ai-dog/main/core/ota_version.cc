#include "core/ota_version.h"

#include <algorithm>
#include <sstream>

#include "core/json_mini.h"

namespace kyle {

std::vector<int> ParseVersion(const std::string& version) {
    std::vector<int> numbers;
    std::stringstream ss(version);
    std::string segment;
    while (std::getline(ss, segment, '.')) {
        numbers.push_back(std::stoi(segment));
    }
    return numbers;
}

bool IsNewVersionAvailable(const std::string& current, const std::string& newer) {
    std::vector<int> cur = ParseVersion(current);
    std::vector<int> nw = ParseVersion(newer);
    for (size_t i = 0; i < std::min(cur.size(), nw.size()); ++i) {
        if (nw[i] > cur[i]) return true;
        if (nw[i] < cur[i]) return false;
    }
    return nw.size() > cur.size();
}

bool ShouldUpgrade(const std::string& current_version, const FirmwareInfo& fw) {
    if (!fw.present) return false;
    if (fw.force) {
        // force 允许「版本相同/更旧也强制刷写」，但版本完全相同（设备已在该版本）时
        // 不再触发下载：否则后端每次 check 都下发 force 固件，设备「重启→下载→重启」
        // 无限循环（实际事故：后台 OTA 记录一直成功、设备却永远在重下同一份固件）。
        // 空版本视为需升级（设备异常无版本信息时 force 兜底刷写）。
        if (current_version.empty() || fw.version.empty()) return true;
        return ParseVersion(current_version) != ParseVersion(fw.version);
    }
    return IsNewVersionAvailable(current_version, fw.version);
}

OtaConfigResponse ParseOtaConfigResponse(const char* json) {
    OtaConfigResponse resp;
    json::Value root;
    if (!json::Parse(json, &root) || !root.IsObject()) return resp;

    // 兼容后端统一返回体 Result<T>（{code,message,data,timestamp}，业务配置在 data 内）
    // 与直接对象两种形态：存在 data 对象时以 data 为解析源。
    const json::Value* obj = &root;
    if (const json::Value* data = root.Get("data"); data && data->IsObject()) {
        obj = data;
    }

    if (const json::Value* ws = obj->Get("websocket"); ws && ws->IsObject()) {
        resp.websocket.present = true;
        if (const json::Value* v = ws->Get("url"); v && v->IsString()) resp.websocket.url = v->AsString();
        if (const json::Value* v = ws->Get("token"); v && v->IsString()) resp.websocket.token = v->AsString();
        if (const json::Value* v = ws->Get("version"); v && v->IsNumber()) resp.websocket.version = static_cast<int>(v->AsNumber());
    }

    if (const json::Value* mq = obj->Get("mqtt"); mq && mq->IsObject()) {
        resp.mqtt.present = true;
        if (const json::Value* v = mq->Get("endpoint"); v && v->IsString()) resp.mqtt.endpoint = v->AsString();
        if (const json::Value* v = mq->Get("client_id"); v && v->IsString()) resp.mqtt.client_id = v->AsString();
        if (const json::Value* v = mq->Get("username"); v && v->IsString()) resp.mqtt.username = v->AsString();
        if (const json::Value* v = mq->Get("password"); v && v->IsString()) resp.mqtt.password = v->AsString();
    }

    if (const json::Value* st = obj->Get("server_time"); st && st->IsObject()) {
        resp.server_time.present = true;
        if (const json::Value* v = st->Get("timestamp"); v && v->IsNumber()) resp.server_time.timestamp_ms = v->AsNumber();
        if (const json::Value* v = st->Get("timezone_offset"); v && v->IsNumber()) resp.server_time.timezone_offset_min = static_cast<int>(v->AsNumber());
    }

    if (const json::Value* fw = obj->Get("firmware"); fw && fw->IsObject()) {
        resp.firmware.present = true;
        if (const json::Value* v = fw->Get("version"); v && v->IsString()) resp.firmware.version = v->AsString();
        if (const json::Value* v = fw->Get("url"); v && v->IsString()) resp.firmware.url = v->AsString();
        if (const json::Value* v = fw->Get("force"); v && v->IsBool()) resp.firmware.force = v->AsBool();
        if (const json::Value* v = fw->Get("force"); v && v->IsNumber()) resp.firmware.force = v->AsNumber() != 0;
    }

    return resp;
}

}  // namespace kyle
