#pragma once

#include "esp_err.h"
#include "media_control.h"
#include "media_state.h"

esp_err_t media_sync_start();
bool media_sync_is_connected(void);
void media_sync_get_snapshot(MediaSnapshot *out);
bool media_sync_consume_dirty(void);
void media_sync_queue_command(media_cmd_t cmd);
void media_sync_queue_start(void);
