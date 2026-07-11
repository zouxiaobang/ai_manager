#pragma once

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief 像素狗状态枚举
 * 定义像素狗的各种行为状态
 */
typedef enum {
    DOG_STATUS_IDLE = 0,     ///< 待机/空闲状态
    DOG_STATUS_WALKING,      ///< 行走状态
    DOG_STATUS_HAPPY,        ///< 开心状态
    DOG_STATUS_SLEEPING,     ///< 睡觉状态
    DOG_STATUS_EATING,       ///< 吃东西状态
    DOG_STATUS_PETTING,      ///< 被抚摸状态
    DOG_STATUS_GREETING,     ///< 打招呼状态
    DOG_STATUS_FOCUS,        ///< 专注状态
    DOG_STATUS_MAX           ///< 状态数量上限
} DogStatus;

/**
 * @brief 像素狗完整状态数据结构
 * 包含等级、经验、陪伴值、情绪等所有属性
 */
typedef struct {
    uint32_t level;               ///< 等级
    uint32_t xp;                  ///< 当前经验值
    uint32_t xp_next;             ///< 升级所需经验值
    uint32_t bond;                ///< 陪伴值（亲密度）
    int8_t emotion;               ///< 情绪值（-100 ~ 100，越高越开心）
    int64_t last_interact_ts;     ///< 上次互动时间戳
    int64_t last_greet_ts;        ///< 上次打招呼时间戳
    DogStatus status;             ///< 当前行为状态
    uint8_t unlocked_items;       ///< 已解锁的物品（位标志）
    uint64_t equipped_items;      ///< 已装备的物品位掩码(bit (id-1)为1表示装备)
} DogState;

/**
 * @brief 初始化像素狗模型
 */
void dog_model_init(void);

/**
 * @brief 获取像素狗当前状态（只读指针）
 * @return 像素狗状态的只读指针
 */
const DogState *dog_model_get(void);

/**
 * @brief 增加经验值
 * @param amount 增加的经验值数量
 */
void dog_model_add_xp(uint32_t amount);

/**
 * @brief 增加陪伴值
 * @param amount 增加的陪伴值数量
 */
void dog_model_add_bond(uint32_t amount);

/**
 * @brief 调整情绪值
 * @param amount 情绪变化量（正数增加，负数减少）
 */
void dog_model_add_emotion(int8_t amount);

/**
 * @brief 摸头互动
 * 用户点击摸头时调用，增加陪伴值和情绪
 */
void dog_model_pet(void);

/**
 * @brief 打招呼互动
 * 用户打招呼时调用，有冷却时间
 */
void dog_model_greet(void);

/**
 * @brief 蹭蹭互动
 * 亲密度较高时解锁，增加情绪较多
 */
void dog_model_nuzzle(void);

/**
 * @brief 抱抱互动
 * 亲密度高时解锁，大幅增加情绪
 */
void dog_model_hug(void);

/**
 * @brief 番茄钟完成回调
 * 专注完成时调用，给予经验值奖励
 * @param duration_minutes 专注时长（分钟）
 */
void dog_model_on_pomodoro_complete(uint32_t duration_minutes);

/**
 * @brief 像素狗时钟滴答
 * 定期调用，用于状态转换和数值衰减
 */
void dog_model_tick(void);

/**
 * @brief 设置像素狗状态
 * @param status 新的状态
 */
void dog_model_set_status(DogStatus status);

/**
 * @brief 应用远程状态（从后端同步）
 * 使用 max 合并逻辑，确保 bond/emotion 不会回退
 * @param remote 远程状态指针
 */
void dog_model_apply_remote_state(const DogState *remote);

/**
 * @brief 覆盖本地状态（从后端互动响应）
 * 直接无条件覆盖所有字段，不进行 max 合并
 * 用于互动后从后端返回的权威数据
 * @param remote 远程状态指针
 */
void dog_model_override_state(const DogState *remote);

/**
 * @brief 获取当前对话内容
 * @return 对话文本指针（静态内存，无需释放）
 */
const char *dog_model_get_speech(void);

#ifdef __cplusplus
}
#endif