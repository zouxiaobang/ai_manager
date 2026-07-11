package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pixel_dog_state")
public class PixelDogState {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer level;

    private Integer xp;

    private Integer xpNext;

    private Integer bond;

    private Integer emotion;

    private Long lastInteractTs;

    private Long lastGreetTs;

    private Integer status;

    private Integer unlockedItems;

    private Long equippedItems;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}