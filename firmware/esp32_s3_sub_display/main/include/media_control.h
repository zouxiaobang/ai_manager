#pragma once

#include <stdbool.h>

#include "media_state.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
  MEDIA_CMD_PREVIOUS = 0,
  MEDIA_CMD_TOGGLE_PLAY_PAUSE,
  MEDIA_CMD_NEXT,
  MEDIA_CMD_START_APP,
} media_cmd_t;

bool media_control_is_playing(void);
bool media_control_is_app_running(void);
bool media_control_is_starting(void);
bool media_control_is_connected(void);
void media_control_set_playing(bool playing);
void media_control_send(media_cmd_t cmd);
void media_control_request_start(void);
void media_control_get_snapshot(MediaSnapshot *out);

#ifdef __cplusplus
}
#endif
