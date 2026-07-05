#pragma once

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define MEDIA_TITLE_MAX 96
#define MEDIA_ARTIST_MAX 64
#define MEDIA_LINE_MAX 128
#define MEDIA_LRC_LINES_MAX 256
#define MEDIA_LRC_REFRESH_MS 50   /* 本地计时器刷新间隔 50ms */

typedef struct {
  int32_t start_ms;
  char text[MEDIA_LINE_MAX];
} LrcLine;

typedef struct {
  bool connected;
  bool app_running;
  bool starting;
  bool playing;
  char title[MEDIA_TITLE_MAX];
  char artist[MEDIA_ARTIST_MAX];

  /* LRC 歌词行列表（从 TF 卡加载或 WS 收到） */
  LrcLine lrc_lines[MEDIA_LRC_LINES_MAX];
  int lrc_line_count;

  /* 本地计时器推算的位置 */
  int32_t position_ms;
  int32_t duration_ms;

  /* 当前显示的歌词行索引 */
  int current_line_index;

  /* 全屏模式显示的 5 行快照 */
  char prev_prev_line[MEDIA_LINE_MAX];
  char prev_line[MEDIA_LINE_MAX];
  char line[MEDIA_LINE_MAX];
  char next_line[MEDIA_LINE_MAX];
  char next_next_line[MEDIA_LINE_MAX];
  int32_t line_start_ms;
  int32_t line_end_ms;

  int64_t updated_at_ms;
} MediaSnapshot;

#ifdef __cplusplus
}
#endif