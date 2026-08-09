#include "core/net_config.h"

namespace kyle {

#ifdef CONFIG_OTA_URL
const char* kDefaultOtaUrl = CONFIG_OTA_URL;
#else
const char* kDefaultOtaUrl = "https://api.tenclass.net/kyle/ota/";
#endif

NetConfig::NetConfig(IStorage& storage, const std::string& default_ota_url)
    : storage_(storage), default_ota_url_(default_ota_url) {}

std::string NetConfig::ota_url() const {
    std::string url = storage_.GetString("wifi", "ota_url");
    if (url.empty()) url = default_ota_url_;
    return url;
}

WebsocketConfig NetConfig::websocket() const {
    WebsocketConfig c;
    c.url = storage_.GetString("websocket", "url");
    c.token = storage_.GetString("websocket", "token");
    c.version = storage_.GetInt("websocket", "version");
    return c;
}

void NetConfig::ApplyOtaResponse(const OtaConfigResponse& resp) {
    if (resp.websocket.present) {
        if (!resp.websocket.url.empty())
            storage_.SetString("websocket", "url", resp.websocket.url);
        if (!resp.websocket.token.empty())
            storage_.SetString("websocket", "token", resp.websocket.token);
        if (resp.websocket.version > 0)
            storage_.SetInt("websocket", "version", resp.websocket.version);
    }
    if (resp.mqtt.present) {
        if (!resp.mqtt.endpoint.empty())
            storage_.SetString("mqtt", "endpoint", resp.mqtt.endpoint);
        if (!resp.mqtt.client_id.empty())
            storage_.SetString("mqtt", "client_id", resp.mqtt.client_id);
        if (!resp.mqtt.username.empty())
            storage_.SetString("mqtt", "username", resp.mqtt.username);
        if (!resp.mqtt.password.empty())
            storage_.SetString("mqtt", "password", resp.mqtt.password);
    }
}

}  // namespace kyle
