package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.entity.PixelDogState;
import com.ai.manager.system.domain.vo.PixelDogStateVO;
import com.ai.manager.system.service.PixelDogStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pixel-dog")
@RequiredArgsConstructor
public class PixelDogStateController {

    private final PixelDogStateService pixelDogStateService;

    @GetMapping("/state")
    public ApiResult<PixelDogStateVO> getState() {
        return ApiResult.ok(pixelDogStateService.getState());
    }

    @PutMapping("/state")
    public ApiResult<PixelDogStateVO> updateState(@jakarta.validation.Valid @RequestBody PixelDogState state) {
        return ApiResult.ok(pixelDogStateService.updateState(state));
    }

    @PostMapping("/xp")
    public ApiResult<PixelDogStateVO> addXp(
            @RequestParam String action,
            @RequestParam int amount) {
        return ApiResult.ok(pixelDogStateService.addXp(action, amount));
    }

    @PostMapping("/interact")
    public ApiResult<PixelDogStateVO> interact(@RequestParam String action) {
        return ApiResult.ok(pixelDogStateService.interact(action));
    }
}