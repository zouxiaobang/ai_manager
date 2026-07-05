#pragma once

#include "esp_err.h"
#include "media_control.h"
#include "media_state.h"

#ifdef __cplusplus
extern "C" {
#endif

esp_err_t media_sync_start();

/** WS 连接状态 */
bool media_sync_is_connected(void);

/** 获取当前快照（含 LRC 行和本地推算的位置） */
void media_sync_get_snapshot(MediaSnapshot *out);

/** 消费脏标记（歌词更新时置脏，UI 层消费后刷新显示） */
bool media_sync_consume_dirty(void);

/** 队列控制命令 */
void media_sync_queue_command(media_cmd_t cmd);
void media_sync_queue_start(void);

/** 尝试手动重连（用于重连按钮） */
void media_sync_request_reconnect(void);

/** 获取重连状态 */
typedef enum {
  MEDIA_WS_IDLE = 0,       /* 空闲/已连接 */
  MEDIA_WS_CONNECTING,     /* 正在连接 */
  MEDIA_WS_RETRYING,       /* 重连中（梯度退避） */
  MEDIA_WS_GAVE_UP,        /* 已放弃重连（7次失败） */
} media_ws_state_t;

media_ws_state_t media_sync_get_ws_state(void);

/**
 * @brief Command execution result tracking
 */
typedef enum {
  MEDIA_CMD_RESULT_IDLE = 0,       // No command pending
  MEDIA_CMD_RESULT_PENDING,        // Command queued, waiting to be sent
  MEDIA_CMD_RESULT_SENDING,        // Command being sent
  MEDIA_CMD_RESULT_SUCCESS,        // Command executed successfully
  MEDIA_CMD_RESULT_FAILED,         // Command execution failed
} media_cmd_result_t;

media_cmd_result_t media_sync_get_cmd_result(void);
media_cmd_result_t media_sync_consume_cmd_result(void);
media_cmd_t media_sync_get_pending_cmd(void);
void media_sync_set_cmd_result(media_cmd_result_t result);

/** 解析 LRC 文本（定义在 media_sync.cpp 中，供 UI 层调用） */
int parse_lrc_lines(const char *lrc_text, LrcLine *out_lines, int max_lines);

/** 从 TF 卡加载缓存的 LRC（定义在 media_sync.cpp 中） */
bool load_lrc_from_sd(char *title_out, size_t title_len,
                      char *lrc_out, size_t lrc_len);

#ifdef __cplusplus
}
#endif