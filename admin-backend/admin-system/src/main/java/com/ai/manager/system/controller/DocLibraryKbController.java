package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.vo.DocLibraryFileVO;
import com.ai.manager.system.domain.vo.DocLibraryStatsVO;
import com.ai.manager.system.service.DocLibraryFileService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/kb")
@RequiredArgsConstructor
public class DocLibraryKbController {

    private final DocLibraryFileService docLibraryFileService;

    @PutMapping("/files/{id}/status")
    public ApiResult<String> toggleKbStatus(@PathVariable Long id) {
        return ApiResult.ok(docLibraryFileService.toggleKbStatus(id));
    }

    @GetMapping("/files")
    public ApiResult<IPage<DocLibraryFileVO>> listKbFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResult.ok(docLibraryFileService.listKbReadyFiles(page, size));
    }

    @GetMapping("/stats")
    public ApiResult<DocLibraryStatsVO> getKbStats() {
        return ApiResult.ok(docLibraryFileService.getKbStats());
    }
}
