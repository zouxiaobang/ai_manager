#pragma once

#include <cstdint>

/**
 * @brief 番茄钟阶段枚举
 * 定义番茄钟的各个状态阶段
 */
enum class PomodoroPhase { 
  Idle,        ///< 空闲状态
  Focus,       ///< 专注中
  ShortBreak,  ///< 短休息
  LongBreak    ///< 长休息
};

/**
 * @brief 待执行阶段枚举
 * 用于阶段切换时的过渡状态
 */
enum class PomodoroPendingPhase { 
  None,        ///< 无待执行
  Work,        ///< 待开始专注
  ShortBreak,  ///< 待开始短休息
  LongBreak    ///< 待开始长休息
};

/**
 * @brief 副屏可触发的 6 种用户动作
 * 定义用户在副屏上可以执行的番茄钟操作
 */
enum class PomodoroUserAction {
  StartFocus,       ///< 开始专注
  PauseFocus,       ///< 暂停专注
  StartShortBreak,  ///< 开始短休息
  PauseShortBreak,  ///< 暂停短休息
  StartLongBreak,   ///< 开始长休息
  PauseLongBreak,   ///< 暂停长休息
};

/**
 * @brief 番茄钟计划配置
 * 定义一个番茄钟计划的各项参数
 */
struct PomodoroPlanConfig {
  int64_t plan_id = 0;                    ///< 计划ID
  int work_duration_min = 25;             ///< 专注时长（分钟）
  int short_break_min = 5;                ///< 短休息时长（分钟）
  int long_break_min = 15;                ///< 长休息时长（分钟）
  int rounds_before_long_break = 4;       ///< 几次专注后进入长休息
  int daily_goal_rounds = 0;              ///< 每日目标轮次
};

/**
 * @brief 番茄钟状态快照
 * 包含番茄钟当前的完整状态，用于UI展示
 */
struct PomodoroSnapshot {
  PomodoroPhase phase = PomodoroPhase::Idle;  ///< 当前阶段
  bool running = false;                        ///< 是否运行中
  int remaining_sec = 0;                       ///< 剩余秒数
  int total_sec = 25 * 60;                     ///< 当前阶段总秒数
  int session_work_rounds = 0;                 ///< 当前会话已完成专注轮次
  int today_work_rounds = 0;                   ///< 今日已完成专注轮次
  int64_t plan_id = 0;                         ///< 当前使用的计划ID
  PomodoroPendingPhase pending = PomodoroPendingPhase::None;  ///< 待执行阶段
  bool backend_connected = false;              ///< 后端是否连接
  bool today_goal_done = false;                ///< 今日目标是否完成
};

/**
 * @brief 远端会话状态（从后端同步）
 * 存储从后端API获取的会话状态
 */
struct PomodoroRemoteSession {
  PomodoroPhase phase = PomodoroPhase::Idle;  ///< 当前阶段
  bool running = false;                        ///< 是否运行中
  bool run_state_idle = true;                  ///< 运行状态是否为空闲
  int remaining_sec = 0;                       ///< 剩余秒数
  int phase_total_sec = 25 * 60;               ///< 阶段总秒数
  int session_work_rounds = 0;                 ///< 会话专注轮次
  int64_t plan_id = 0;                         ///< 计划ID
  PomodoroPendingPhase pending = PomodoroPendingPhase::None;  ///< 待执行阶段
  int64_t synced_at_ms = 0;                    ///< 同步时间戳（毫秒）
  bool controller_is_device = false;           ///< 控制方是否为设备端
  bool valid = false;                          ///< 数据是否有效
};

/**
 * @brief 同步请求负载
 * 用于向后端上报番茄钟状态的数据结构
 */
struct PomodoroSyncPayload {
  const char *phase;              ///< 阶段字符串
  const char *run_state;          ///< 运行状态字符串
  int remaining_sec;              ///< 剩余秒数
  int phase_total_sec;            ///< 阶段总秒数
  int session_work_rounds;        ///< 会话专注轮次
  int64_t plan_id;                ///< 计划ID
  const char *pending_phase;      ///< 待执行阶段
  bool take_control;              ///< 是否抢占控制权
};

/**
 * @brief 初始化番茄钟模型
 */
void pomodoro_init();

/**
 * @brief 番茄钟时钟滴答
 * 每秒调用一次，用于倒计时
 */
void pomodoro_tick();

/**
 * @brief 获取番茄钟当前状态快照
 * @return 当前状态快照
 */
PomodoroSnapshot pomodoro_get();

/**
 * @brief 获取当前使用的计划配置
 * @return 计划配置
 */
PomodoroPlanConfig pomodoro_get_plan();

/**
 * @brief 应用一个番茄钟计划
 * @param plan 计划配置
 */
void pomodoro_apply_plan(const PomodoroPlanConfig &plan);

/**
 * @brief 设置今日已完成的专注轮次
 * @param rounds 轮次数
 */
void pomodoro_set_today_work_rounds(int rounds);

/**
 * @brief 设置后端连接状态
 * @param connected 是否已连接
 */
void pomodoro_set_backend_connected(bool connected);

/**
 * @brief 今日目标是否已完成
 * @return true 表示今日轮次已满，显示结束态且禁止操作
 */
bool pomodoro_is_today_goal_done();

/**
 * @brief 操作是否被阻止
 * @return true 表示当前不可操作
 */
bool pomodoro_is_operation_blocked();

/**
 * @brief 当前阶段任务是否已完成
 * @return true 表示专注/休息/今日任务已完成，专注模式可退出
 */
bool pomodoro_is_current_task_complete();

/**
 * @brief 卡片点击动作
 * 按当前阶段切换开始/暂停（6 种动作之一）
 */
void pomodoro_card_action();

/**
 * @brief 应用用户动作
 * @param action 用户动作枚举
 */
void pomodoro_apply_user_action(PomodoroUserAction action);

/**
 * @brief 重置番茄钟到空闲状态
 */
void pomodoro_reset();

/**
 * @brief 获取并消费待上报的专注完成记录
 * @param duration_sec 输出参数，专注时长（秒）
 * @return true 表示有有效记录需要上报
 */
bool pomodoro_consume_work_record_request(int *duration_sec);

/**
 * @brief 锁屏时番茄钟是否处于活动状态
 * @return true 表示活动中
 */
bool pomodoro_is_active_on_lock();

/**
 * @brief 构建同步负载数据
 * @param out 输出参数，同步负载
 * @return true 表示构建成功
 */
bool pomodoro_build_sync_payload(PomodoroSyncPayload *out);

/**
 * @brief 应用远端会话状态
 * @param remote 远端会话状态
 * @param force 是否强制应用（忽略本地更新时间）
 */
void pomodoro_apply_remote_session(const PomodoroRemoteSession &remote, bool force);

/**
 * @brief 标记同步脏数据（需要上报）
 * @param take_control 是否同时抢占控制权
 */
void pomodoro_mark_sync_dirty(bool take_control);

/**
 * @brief 消费同步脏数据标记
 * @param take_control_out 输出参数，是否需要抢占控制权
 * @return true 表示有脏数据需要同步
 */
bool pomodoro_consume_sync_dirty(bool *take_control_out);

/**
 * @brief 获取上次应用同步的时间戳
 * @return 时间戳（毫秒）
 */
int64_t pomodoro_last_applied_sync_ms();

/**
 * @brief 是否应该应用远端拉取的数据
 * 本地刚操作后短暂忽略远端 pull，避免被旧会话覆盖
 * @return true 表示可以应用
 */
bool pomodoro_should_apply_remote_pull();
