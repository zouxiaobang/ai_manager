package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class PixelDogStateVO {

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
}