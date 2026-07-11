#include "pixel_dog_model.h"

#include <algorithm>
#include <cstdlib>
#include <mutex>

#include "esp_log.h"
#include "esp_timer.h"
#include "nvs_flash.h"
#include "pixel_dog_sync.h"
#include "pomodoro_model.h"

namespace {
constexpr char TAG[] = "pixel_dog";
constexpr char kNvsNamespace[] = "pixel_dog";

std::mutex g_mu;
DogState g_state = {};

constexpr int64_t kThreeDaysSec = 3 * 24 * 60 * 60LL;
constexpr int64_t kThirtyMinutesSec = 30 * 60LL;
constexpr int64_t kOneHourSec = 60 * 60LL;
constexpr uint32_t kMaxBond = 100;
constexpr int kMaxEmotion = 100;
constexpr int kMinEmotion = -100;

uint32_t xp_for_level(uint32_t level) {
    return level * 100 + (level - 1) * 50;
}

void save_state_locked() {
    nvs_handle_t handle;
    esp_err_t err = nvs_open(kNvsNamespace, NVS_READWRITE, &handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "NVS open failed: %s", esp_err_to_name(err));
        return;
    }

    err = nvs_set_u32(handle, "level", g_state.level);
    if (err != ESP_OK) ESP_LOGE(TAG, "NVS save level failed: %s", esp_err_to_name(err));

    err = nvs_set_u32(handle, "xp", g_state.xp);
    if (err != ESP_OK) ESP_LOGE(TAG, "NVS save xp failed: %s", esp_err_to_name(err));

    err = nvs_set_u32(handle, "bond", g_state.bond);
    if (err != ESP_OK) ESP_LOGE(TAG, "NVS save bond failed: %s", esp_err_to_name(err));

    err = nvs_set_i8(handle, "emotion", g_state.emotion);
    if (err != ESP_OK) ESP_LOGE(TAG, "NVS save emotion failed: %s", esp_err_to_name(err));

    err = nvs_set_i64(handle, "last_interact", g_state.last_interact_ts);
    if (err != ESP_OK) ESP_LOGE(TAG, "NVS save last_interact failed: %s", esp_err_to_name(err));

    err = nvs_set_i64(handle, "last_greet", g_state.last_greet_ts);
    if (err != ESP_OK) ESP_LOGE(TAG, "NVS save last_greet failed: %s", esp_err_to_name(err));

    err = nvs_set_u8(handle, "unlocked", g_state.unlocked_items);
    if (err != ESP_OK) ESP_LOGE(TAG, "NVS save unlocked failed: %s", esp_err_to_name(err));

    err = nvs_commit(handle);
    if (err != ESP_OK) ESP_LOGE(TAG, "NVS commit failed: %s", esp_err_to_name(err));

    nvs_close(handle);
}

void load_state_locked() {
    nvs_handle_t handle;
    esp_err_t err = nvs_open(kNvsNamespace, NVS_READONLY, &handle);
    if (err != ESP_OK) {
        ESP_LOGW(TAG, "NVS open failed, using defaults: %s", esp_err_to_name(err));
        return;
    }

    uint32_t val32;
    int8_t val8;
    int64_t val64;

    err = nvs_get_u32(handle, "level", &val32);
    if (err == ESP_OK) g_state.level = val32;

    err = nvs_get_u32(handle, "xp", &val32);
    if (err == ESP_OK) g_state.xp = val32;

    err = nvs_get_u32(handle, "bond", &val32);
    if (err == ESP_OK) g_state.bond = val32;

    err = nvs_get_i8(handle, "emotion", &val8);
    if (err == ESP_OK) g_state.emotion = val8;

    err = nvs_get_i64(handle, "last_interact", &val64);
    if (err == ESP_OK) g_state.last_interact_ts = val64;

    err = nvs_get_i64(handle, "last_greet", &val64);
    if (err == ESP_OK) g_state.last_greet_ts = val64;

    err = nvs_get_u8(handle, "unlocked", reinterpret_cast<uint8_t*>(&val32));
    if (err == ESP_OK) g_state.unlocked_items = val32;

    nvs_close(handle);
}

void check_upgrade_locked() {
    while (g_state.xp >= g_state.xp_next) {
        g_state.xp -= g_state.xp_next;
        g_state.level++;
        g_state.xp_next = xp_for_level(g_state.level);
        g_state.emotion = std::min(kMaxEmotion, g_state.emotion + 30);

        if (g_state.level >= 5) {
            g_state.unlocked_items |= 0x01;
        }
        if (g_state.level >= 10) {
            g_state.unlocked_items |= 0x02;
        }
        if (g_state.level >= 15) {
            g_state.unlocked_items |= 0x04;
        }
        if (g_state.level >= 20) {
            g_state.unlocked_items |= 0x08;
        }

        ESP_LOGI(TAG, "Level up! Now level %u", g_state.level);
        save_state_locked();
    }
}

int64_t now_sec() {
    return esp_timer_get_time() / 1000000LL;
}
}  // namespace

void dog_model_init(void) {
    std::lock_guard<std::mutex> lock(g_mu);

    g_state.level = 1;
    g_state.xp = 0;
    g_state.xp_next = xp_for_level(1);
    g_state.bond = 0;
    g_state.emotion = 0;
    g_state.last_interact_ts = now_sec();
    g_state.last_greet_ts = 0;
    g_state.status = DOG_STATUS_IDLE;
    g_state.unlocked_items = 0x01;
    g_state.equipped_items = 0;

    load_state_locked();
    check_upgrade_locked();

    ESP_LOGI(TAG, "Pixel dog initialized: level=%u, xp=%u/%u, bond=%u, emotion=%d",
             g_state.level, g_state.xp, g_state.xp_next, g_state.bond, g_state.emotion);
}

const DogState *dog_model_get(void) {
    std::lock_guard<std::mutex> lock(g_mu);
    static DogState copy;
    copy = g_state;
    return &copy;
}

void dog_model_add_xp(uint32_t amount) {
    std::lock_guard<std::mutex> lock(g_mu);
    g_state.xp += amount;
    g_state.last_interact_ts = now_sec();
    check_upgrade_locked();
    save_state_locked();
    ESP_LOGI(TAG, "Added %u XP, now %u/%u", amount, g_state.xp, g_state.xp_next);
    dog_sync_mark_dirty();
}

void dog_model_add_bond(uint32_t amount) {
    std::lock_guard<std::mutex> lock(g_mu);
    g_state.bond = std::min(kMaxBond, g_state.bond + amount);
    g_state.last_interact_ts = now_sec();
    save_state_locked();
    ESP_LOGI(TAG, "Added %u bond, now %u/%u", amount, g_state.bond, kMaxBond);
    dog_sync_mark_dirty();
}

void dog_model_add_emotion(int8_t amount) {
    std::lock_guard<std::mutex> lock(g_mu);
    g_state.emotion = std::max(kMinEmotion, std::min(kMaxEmotion, g_state.emotion + amount));
    save_state_locked();
}

void dog_model_pet(void) {
    std::lock_guard<std::mutex> lock(g_mu);
    g_state.emotion = std::min(kMaxEmotion, g_state.emotion + 10);
    g_state.bond = std::min(kMaxBond, g_state.bond + 1);
    g_state.last_interact_ts = now_sec();
    save_state_locked();
    ESP_LOGI(TAG, "Pet! emotion=%d, bond=%u", g_state.emotion, g_state.bond);
    dog_sync_mark_dirty();
}

void dog_model_greet(void) {
    std::lock_guard<std::mutex> lock(g_mu);
    g_state.bond = std::min(kMaxBond, g_state.bond + 5);
    g_state.emotion = std::min(kMaxEmotion, g_state.emotion + 5);
    g_state.last_greet_ts = now_sec();
    g_state.last_interact_ts = now_sec();
    save_state_locked();
    ESP_LOGI(TAG, "Greet! bond=%u", g_state.bond);
    dog_sync_mark_dirty();
}

void dog_model_nuzzle(void) {
    std::lock_guard<std::mutex> lock(g_mu);
    g_state.emotion = std::min(kMaxEmotion, g_state.emotion + 15);
    g_state.bond = std::min(kMaxBond, g_state.bond + 3);
    g_state.last_interact_ts = now_sec();
    save_state_locked();
    ESP_LOGI(TAG, "Nuzzle! emotion=%d, bond=%u", g_state.emotion, g_state.bond);
    dog_sync_mark_dirty();
}

void dog_model_hug(void) {
    std::lock_guard<std::mutex> lock(g_mu);
    g_state.emotion = std::min(kMaxEmotion, g_state.emotion + 30);
    g_state.bond = std::min(kMaxBond, g_state.bond + 5);
    g_state.last_interact_ts = now_sec();
    save_state_locked();
    ESP_LOGI(TAG, "Hug! emotion=%d, bond=%u", g_state.emotion, g_state.bond);
    dog_sync_mark_dirty();
}

void dog_model_on_pomodoro_complete(uint32_t duration_minutes) {
    uint32_t xp_reward = 0;
    uint32_t bond_reward = 0;

    if (duration_minutes >= 20) {
        xp_reward = 15;
        bond_reward = 2;
    } else if (duration_minutes >= 10) {
        xp_reward = 5;
        bond_reward = 1;
    } else {
        xp_reward = 3;
        bond_reward = 1;
    }

    std::lock_guard<std::mutex> lock(g_mu);
    g_state.xp += xp_reward;
    g_state.bond = std::min(kMaxBond, g_state.bond + bond_reward);
    g_state.emotion = std::min(kMaxEmotion, g_state.emotion + 5);
    g_state.last_interact_ts = now_sec();

    check_upgrade_locked();
    save_state_locked();

    ESP_LOGI(TAG, "Pomodoro complete! +%u XP, +%u bond for %u min", xp_reward, bond_reward, duration_minutes);
    dog_sync_mark_dirty();
}

void dog_model_tick(void) {
    std::lock_guard<std::mutex> lock(g_mu);
    const int64_t now = now_sec();

    if (now - g_state.last_interact_ts > kThreeDaysSec) {
        g_state.emotion = std::max(kMinEmotion, g_state.emotion - 1);
        if ((now - g_state.last_interact_ts) % 86400 == 0) {
            ESP_LOGI(TAG, "Missing you... emotion=%d", g_state.emotion);
        }
    }

    if (now - g_state.last_interact_ts > kThirtyMinutesSec) {
        const int64_t elapsed = now - g_state.last_interact_ts - kThirtyMinutesSec;
        const int decay_minutes = static_cast<int>(elapsed / 60LL);
        if (decay_minutes > 0) {
            g_state.emotion = std::max(kMinEmotion, g_state.emotion - decay_minutes);
        }
    }

    if (now - g_state.last_interact_ts > kOneHourSec) {
        const int64_t elapsed = now - g_state.last_interact_ts - kOneHourSec;
        const int decay_hours = static_cast<int>(elapsed / 3600LL);
        if (decay_hours > 0) {
            g_state.bond = g_state.bond > decay_hours ? g_state.bond - decay_hours : 0;
        }
    }

    PomodoroSnapshot pomo = pomodoro_get();
    bool should_focus = (pomo.phase == PomodoroPhase::Focus && pomo.running);

    if (should_focus) {
        if (g_state.status != DOG_STATUS_FOCUS) {
            g_state.status = DOG_STATUS_FOCUS;
            dog_sync_mark_dirty();
            ESP_LOGI(TAG, "Focus mode");
        }
    } else {
        if (g_state.status != DOG_STATUS_IDLE) {
            g_state.status = DOG_STATUS_IDLE;
            ESP_LOGI(TAG, "Idle mode");
        }
    }
}

void dog_model_set_status(DogStatus status) {
    std::lock_guard<std::mutex> lock(g_mu);
    g_state.status = status;
}

void dog_model_apply_remote_state(const DogState *remote) {
    if (remote == nullptr) {
        return;
    }
    std::lock_guard<std::mutex> lock(g_mu);

    // 夜间（22:00-6:00）本地为睡眠状态时，不覆盖远程状态
    const int64_t now = now_sec();
    const int hour = (now % 86400) / 3600;
    const bool is_sleep_hours = (hour >= 22 || hour < 6);

    g_state.level = remote->level;
    g_state.xp = remote->xp;
    g_state.xp_next = remote->xp_next;
    // 后端为权威数据源，直接覆盖 bond/emotion
    // 本地与远程的数据一致性由后端负责维护
    g_state.bond = remote->bond;
    g_state.emotion = remote->emotion;
    // 不同步远程互动时间戳，避免覆盖本地后导致 dog_model_tick 误判为长期无互动而触发衰减
    g_state.unlocked_items = remote->unlocked_items;
    g_state.equipped_items = remote->equipped_items;

    // 睡眠时间保留本地睡眠状态，防止远程覆盖导致反复切换
    if (is_sleep_hours && g_state.status == DOG_STATUS_SLEEPING) {
        // keep sleeping
    } else if (g_state.status == DOG_STATUS_FOCUS) {
        // 本地专注中保留焦点状态，防止被远程旧状态(IDLE)覆盖
        ESP_LOGD(TAG, "Focus active, keeping local status");
    } else {
        g_state.status = remote->status;
    }

    save_state_locked();
    ESP_LOGD(TAG, "Applied remote state: level=%u, xp=%u/%u, bond=%u",
             g_state.level, g_state.xp, g_state.xp_next, g_state.bond);
}

void dog_model_override_state(const DogState *remote) {
    if (remote == nullptr) {
        return;
    }
    std::lock_guard<std::mutex> lock(g_mu);

    g_state.level = remote->level;
    g_state.xp = remote->xp;
    g_state.xp_next = remote->xp_next;
    g_state.bond = remote->bond;
    g_state.emotion = remote->emotion;
    g_state.last_interact_ts = remote->last_interact_ts;
    g_state.last_greet_ts = remote->last_greet_ts;
    g_state.unlocked_items = remote->unlocked_items;
    g_state.equipped_items = remote->equipped_items;
    g_state.status = remote->status;

    save_state_locked();
    ESP_LOGI(TAG, "Override state from interaction: level=%u, xp=%u/%u, bond=%u, emotion=%d",
             g_state.level, g_state.xp, g_state.xp_next, g_state.bond, g_state.emotion);
}

const char *dog_model_get_speech(void) {
    std::lock_guard<std::mutex> lock(g_mu);

    const int64_t now = now_sec();
    const int64_t days_since_interact = (now - g_state.last_interact_ts) / 86400;

    // 长时间没互动（≥3天）- 类似PC版互动提醒
    if (days_since_interact >= 3) {
        static const char *long_away[] = {
            "最近是不是很忙呀？",
            "你好像很久没来看我了...",
            "想你了...",
            "我好孤单...",
        };
        return long_away[rand() % (sizeof(long_away) / sizeof(long_away[0]))];
    }

    const int emotion = g_state.emotion;
    const int bond = g_state.bond;

    // 与PC端一致的亲密度公式: (emotion+100)/2 * 0.4 + bond * 0.6
    // 整数形式 *10: intimacy_x10 = (emotion+100)*2 + bond*6  (范围0~1000)
    const int intimacy_x10 = (emotion + 100) * 2 + bond * 6;

    if (bond < 20) {
        if (emotion >= 20) {
            static const char *list[] = {
                "今天天气不错",
                "阳光真好",
                "发呆中...",
                "看着你工作",
                "安静地待着",
            };
            return list[rand() % 5];
        } else if (emotion >= 0) {
            static const char *list[] = {
                "...",
                "你好",
                "嗯",
                "随便吧...",
                "你在忙吗?",
            };
            return list[rand() % 5];
        } else if (emotion >= -30) {
            static const char *list[] = {
                "一个人好寂寞...",
                "你好像不太喜欢我...",
                "我是不是做错什么了?",
                "好孤单，没人理我...",
                "也许我不该在这里...",
            };
            return list[rand() % 5];
        } else {
            static const char *list[] = {
                "你根本不在乎我！",
                "既然这样，算了...",
                "讨厌你！",
                "再也不理你了！",
                "哼！走了！",
            };
            return list[rand() % 5];
        }
    } else if (intimacy_x10 < 600) {
        if (emotion >= 20) {
            static const char *list[] = {
                "今天过得不错！",
                "一起加油吧！",
                "你在忙什么呢？",
                "记得休息哦～",
                "继续努力！",
            };
            return list[rand() % 5];
        } else {
            static const char *list[] = {
                "有点无聊呢...",
                "你多久没理我了...",
                "希望你能陪陪我",
                "今天好安静",
                "想出去玩...",
            };
            return list[rand() % 5];
        }
    } else if (intimacy_x10 < 850) {
        if (emotion >= 10) {
            static const char *list[] = {
                "蹭蹭你的手～",
                "撒娇卖萌中～",
                "最喜欢你了！",
                "想一直陪着你",
                "黏人模式开启～",
            };
            return list[rand() % 5];
        } else {
            static const char *list[] = {
                "不要不理我嘛...",
                "你去哪里了?",
                "我好想你...",
                "没有你我好难过",
                "回来好不好?",
            };
            return list[rand() % 5];
        }
    } else {
        if (emotion >= 0) {
            static const char *list[] = {
                "紧紧抱住你！",
                "在你怀里蹭来蹭去～",
                "幸福地闭上眼睛",
                "你是全世界最好的！",
                "永远爱你！",
            };
            return list[rand() % 5];
        } else {
            static const char *list[] = {
                "你不要离开我...",
                "紧紧抓住你的衣角",
                "眼眶红红的看着你",
                "在你怀里委屈地哭",
                "不要不理我，我会害怕...",
            };
            return list[rand() % 5];
        }
    }
}