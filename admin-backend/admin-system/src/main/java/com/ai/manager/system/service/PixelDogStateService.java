package com.ai.manager.system.service;

import com.ai.manager.system.domain.entity.PixelDogState;
import com.ai.manager.system.domain.vo.PixelDogStateVO;

public interface PixelDogStateService {

    PixelDogStateVO getState();

    PixelDogStateVO updateState(PixelDogState state);

    PixelDogState getActiveEntity();

    PixelDogStateVO addXp(String action, int xpAmount);

    PixelDogStateVO interact(String action);
}