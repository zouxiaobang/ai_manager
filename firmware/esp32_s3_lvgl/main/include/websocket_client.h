#pragma once

#include "esp_err.h"

esp_err_t websocket_start_playback_session();
void websocket_close_playback_session(const char *reason_type);
void websocket_send_control_command(const char *command);
void websocket_send_seek(int position_ms);
