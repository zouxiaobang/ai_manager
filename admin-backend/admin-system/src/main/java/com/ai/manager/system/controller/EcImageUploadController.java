package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.vo.EcImageUploadVO;
import com.ai.manager.system.service.EcImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ecommerce/images")
@RequiredArgsConstructor
public class EcImageUploadController {

    private final EcImageUploadService ecImageUploadService;

    @PostMapping("/upload")
    public ApiResult<EcImageUploadVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResult.ok(ecImageUploadService.uploadEcommerceImage(file));
    }
}
