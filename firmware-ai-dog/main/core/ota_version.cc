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
    if (fw.force) return true;
    return IsNewVersionAvailable(current_version, fw.version);
}

OtaConfigResponse ParseOtaConfigResponse(const char* json) {
    OtaConfigResponse resp;
    json::Value root;
    if (!json::Parse(json, &root) || !root.IsObject()) return resp;

    if (const json::Value* ws = root.Get("websocket"); ws && ws->IsObject()) {
        resp.websocket.present = true;
        if (const json::Value* v = ws->Get("url"); v && v->IsString()) resp.websocket.url = v->AsString();
        if (const json::Value* v = ws->Get("token"); v && v->IsString()) resp.websocket.token = v->AsString();
        if (const json::Value* v = ws->Get("version"); v && v->IsNumber()) resp.websocket.version = static_cast<int>(v->AsNumber());
    }

    if (const json::Value* mq = root.Get("mqtt"); mq && mq->IsObject()) {
        resp.mqtt.present = true;
        if (const json::Value* v = mq->Get("endpoint"); v && v->IsString()) resp.mqtt.endpoint = v->AsString();
        if (const json::Value* v = mq->Get("client_id"); v && v->IsString()) resp.mqtt.client_id = v->AsString();
        if (const json::Value* v = mq->Get("username"); v && v->IsString()) resp.mqtt.username = v->AsString();
        if (const json::Value* v = mq->Get("password"); v && v->IsString()) resp.mqtt.password = v->AsString();
    }

    if (const json::Value* st = root.Get("server_time"); st && st->IsObject()) {
        resp.server_time.present = true;
        if (const json::Value* v = st->Get("timestamp"); v && v->IsNumber()) resp.server_time.timestamp_ms = v->AsNumber();
        if (const json::Value* v = st->Get("timezone_offset"); v && v->IsNumber()) resp.server_time.timezone_offset_min = static_cast<int>(v->AsNumber());
    }

    if (const json::Value* fw = root.Get("firmware"); fw && fw->IsObject()) {
        resp.firmware.present = true;
        if (const json::Value* v = fw->Get("version"); v && v->IsString()) resp.firmware.version = v->AsString();
        if (const json::Value* v = fw->Get("url"); v && v->IsString()) resp.firmware.url = v->AsString();
        if (const json::Value* v = fw->Get("force"); v && v->IsBool()) resp.firmware.force = v->AsBool();
        if (const json::Value* v = fw->Get("force"); v && v->IsNumber()) resp.firmware.force = v->AsNumber() != 0;
    }

    return resp;
}

}  // namespace kyle
