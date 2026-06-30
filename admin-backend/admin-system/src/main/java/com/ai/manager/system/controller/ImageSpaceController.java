package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.ImageSpaceRenameRequest;
import com.ai.manager.system.domain.vo.ImageSpaceCategoryNodeVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageDetailVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageItemVO;
import com.ai.manager.system.domain.vo.ImageSpaceNameCheckVO;
import com.ai.manager.system.domain.vo.ImageSpaceNormalizeResultVO;
import com.ai.manager.system.service.ImageSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/image-space")
@RequiredArgsConstructor
public class ImageSpaceController {

    private final ImageSpaceService imageSpaceService;

    @GetMapping("/categories")
    public ApiResult<List<ImageSpaceCategoryNodeVO>> categories() {
        return ApiResult.ok(imageSpaceService.listCategories());
    }

    @GetMapping("/images")
    public ApiResult<PageResult<ImageSpaceImageItemVO>> images(
            @RequestParam(defaultValue = "ECOMMERCE_IMAGES") String zone,
            @RequestParam(defaultValue = "all") String categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return ApiResult.ok(imageSpaceService.pageImages(zone, categoryId, keyword, page, pageSize));
    }

    @GetMapping("/images/detail")
    public ApiResult<ImageSpaceImageDetailVO> imageDetail(
            @RequestParam String zone,
            @RequestParam String relativePath
    ) {
        return ApiResult.ok(imageSpaceService.getImageDetail(zone, relativePath));
    }

    @GetMapping("/images/check-name")
    public ApiResult<ImageSpaceNameCheckVO> checkName(
            @RequestParam String zone,
            @RequestParam String relativePath,
            @RequestParam String newFileName
    ) {
        return ApiResult.ok(imageSpaceService.checkFileName(zone, relativePath, newFileName));
    }

    @PutMapping("/images/rename")
    public ApiResult<ImageSpaceImageDetailVO> rename(@RequestBody ImageSpaceRenameRequest request) {
        return ApiResult.ok(imageSpaceService.renameImage(request));
    }

    @DeleteMapping("/images")
    public ApiResult<Void> delete(
            @RequestParam String zone,
            @RequestParam String relativePath
    ) {
        imageSpaceService.deleteImage(zone, relativePath);
        return ApiResult.ok();
    }

    @PostMapping("/normalize-ecommerce-names")
    public ApiResult<ImageSpaceNormalizeResultVO> normalizeEcommerceNames(
            @RequestParam(defaultValue = "true") boolean dryRun
    ) {
        return ApiResult.ok(imageSpaceService.normalizeEcommerceImageNames(dryRun));
    }
}
