#include "pomodoro_plan_cache.h"

#include "esp_log.h"
#include "nvs.h"
#include "nvs_flash.h"

namespace {
constexpr char TAG[] = "pomo_plan";
constexpr char kNs[] = "pomo_plan";
constexpr char kKeyPlanId[] = "plan_id";
constexpr char kKeyWork[] = "work_min";
constexpr char kKeyShort[] = "short_min";
constexpr char kKeyLong[] = "long_min";
constexpr char kKeyRounds[] = "rounds";
constexpr char kKeyDailyGoal[] = "daily_goal";
}  // namespace

esp_err_t pomodoro_plan_cache_load(PomodoroPlanConfig *out) {
  if (out == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  *out = PomodoroPlanConfig{};

  nvs_handle_t handle = 0;
  esp_err_t err = nvs_open(kNs, NVS_READONLY, &handle);
  if (err == ESP_ERR_NVS_NOT_FOUND) {
    ESP_LOGI(TAG, "No cached plan, use defaults");
    return ESP_OK;
  }
  if (err != ESP_OK) {
    return err;
  }

  int64_t plan_id = 0;
  int32_t work = 25;
  int32_t short_min = 5;
  int32_t long_min = 15;
  int32_t rounds = 4;
  int32_t daily_goal = 0;

  nvs_get_i64(handle, kKeyPlanId, &plan_id);
  nvs_get_i32(handle, kKeyWork, &work);
  nvs_get_i32(handle, kKeyShort, &short_min);
  nvs_get_i32(handle, kKeyLong, &long_min);
  nvs_get_i32(handle, kKeyRounds, &rounds);
  nvs_get_i32(handle, kKeyDailyGoal, &daily_goal);
  nvs_close(handle);

  out->plan_id = plan_id;
  out->work_duration_min = work > 0 ? work : 25;
  out->short_break_min = short_min > 0 ? short_min : 5;
  out->long_break_min = long_min > 0 ? long_min : 15;
  out->rounds_before_long_break = rounds > 0 ? rounds : 4;
  out->daily_goal_rounds = daily_goal > 0 ? daily_goal : 0;

  ESP_LOGI(TAG, "Loaded plan id=%lld work=%d", static_cast<long long>(out->plan_id),
           out->work_duration_min);
  return ESP_OK;
}

esp_err_t pomodoro_plan_cache_save(const PomodoroPlanConfig &plan) {
  nvs_handle_t handle = 0;
  esp_err_t err = nvs_open(kNs, NVS_READWRITE, &handle);
  if (err != ESP_OK) {
    return err;
  }

  nvs_set_i64(handle, kKeyPlanId, plan.plan_id);
  nvs_set_i32(handle, kKeyWork, plan.work_duration_min);
  nvs_set_i32(handle, kKeyShort, plan.short_break_min);
  nvs_set_i32(handle, kKeyLong, plan.long_break_min);
  nvs_set_i32(handle, kKeyRounds, plan.rounds_before_long_break);
  nvs_set_i32(handle, kKeyDailyGoal, plan.daily_goal_rounds);
  err = nvs_commit(handle);
  nvs_close(handle);

  if (err == ESP_OK) {
    ESP_LOGI(TAG, "Saved plan id=%lld", static_cast<long long>(plan.plan_id));
  }
  return err;
}
