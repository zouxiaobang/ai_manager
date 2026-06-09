#pragma once

#include "esp_err.h"
#include <lvgl.h>

#include "pomodoro_api_client.h"

namespace pomodoro_module {

esp_err_t create(lv_obj_t *tile);
void on_show();
void on_hide();
void request_plan_fetch();
/** 拉取计划 + 恢复后端会话（ESP 重启后调用） */
void request_bootstrap();

void request_start_work();
void request_start_pending_phase();
bool is_pending_phase();

/** 远端会话与当前 UI 状态一致时返回 true，用于跳过无效 apply/重绘 */
bool remote_session_matches_local(const PomodoroRemoteSession *remote);

/** WORK 记录写入后检查今日计划是否完成（异步调用） */
void check_today_goal_after_work_record();

}  // namespace pomodoro_module
