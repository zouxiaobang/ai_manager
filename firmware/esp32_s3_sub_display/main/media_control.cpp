#include "media_control.h"

#include "esp_log.h"
#include "media_sync.h"

namespace {
constexpr char TAG[] = "media_ctrl";
bool s_playing = false;
}  // namespace

extern "C" {

bool media_control_is_playing(void) {
  return s_playing;
}

bool media_control_is_app_running(void) {
  MediaSnapshot snap = {};
  media_sync_get_snapshot(&snap);
  return snap.app_running;
}

bool media_control_is_starting(void) {
  MediaSnapshot snap = {};
  media_sync_get_snapshot(&snap);
  return snap.starting;
}

bool media_control_is_connected(void) {
  return media_sync_is_connected();
}

void media_control_set_playing(bool playing) {
  s_playing = playing;
}

void media_control_send(media_cmd_t cmd) {
  switch (cmd) {
    case MEDIA_CMD_PREVIOUS:
      ESP_LOGI(TAG, "control: previous");
      break;
    case MEDIA_CMD_TOGGLE_PLAY_PAUSE:
      ESP_LOGI(TAG, "control: toggle");
      break;
    case MEDIA_CMD_NEXT:
      ESP_LOGI(TAG, "control: next");
      break;
    case MEDIA_CMD_START_APP:
      ESP_LOGI(TAG, "control: start app");
      break;
  }
  media_sync_queue_command(cmd);
}

void media_control_request_start(void) {
  media_sync_queue_start();
}

void media_control_get_snapshot(MediaSnapshot *out) {
  media_sync_get_snapshot(out);
}

}  // extern "C"
