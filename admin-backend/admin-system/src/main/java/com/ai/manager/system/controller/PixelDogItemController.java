package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.entity.PixelDogItem;
import com.ai.manager.system.domain.vo.PixelDogItemVO;
import com.ai.manager.system.service.PixelDogItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pixel-dog/items")
@RequiredArgsConstructor
public class PixelDogItemController {
    private final PixelDogItemService pixelDogItemService;

    @GetMapping
    public ApiResult<List<PixelDogItemVO>> listItems() {
        return ApiResult.ok(pixelDogItemService.listItems());
    }

    @PostMapping
    public ApiResult<PixelDogItemVO> createItem(@RequestBody PixelDogItem item) {
        return ApiResult.ok(pixelDogItemService.createItem(item));
    }

    @PutMapping("/{id}")
    public ApiResult<PixelDogItemVO> updateItem(@PathVariable Long id, @RequestBody PixelDogItem item) {
        item.setId(id);
        return ApiResult.ok(pixelDogItemService.updateItem(item));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteItem(@PathVariable Long id) {
        pixelDogItemService.deleteItem(id);
        return ApiResult.ok(null);
    }
}
