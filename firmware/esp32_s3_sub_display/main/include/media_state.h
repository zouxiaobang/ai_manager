#pragma once

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define MEDIA_TITLE_MAX 96
#define MEDIA_ARTIST_MAX 64
#define MEDIA_LINE_MAX 128

typedef struct {
  bool connected;
  bool app_running;
  bool starting;
  bool playing;
  char title[MEDIA_TITLE_MAX];
  char artist[MEDIA_ARTIST_MAX];
  char prev_line[MEDIA_LINE_MAX];
  char line[MEDIA_LINE_MAX];
  char next_line[MEDIA_LINE_MAX];
  int32_t position_ms;
  int32_t duration_ms;
  int32_t line_start_ms;
  int32_t line_end_ms;
  int64_t updated_at_ms;
} MediaSnapshot;

#ifdef __cplusplus
}
#endif
