package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.vo.DocLibraryTrashItemVO;
import com.ai.manager.system.service.DocLibraryFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library/trash")
@RequiredArgsConstructor
public class DocLibraryTrashController {

    private final DocLibraryFileService docLibraryFileService;

    @GetMapping
    public ApiResult<List<DocLibraryTrashItemVO>> listTrash() {
        return ApiResult.ok(docLibraryFileService.listTrash());
    }

    @PostMapping("/{id}/restore")
    public ApiResult<Void> restoreFile(@PathVariable Long id) {
        docLibraryFileService.restoreFile(id);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> purgeFile(@PathVariable Long id) {
        docLibraryFileService.purgeFile(id);
        return ApiResult.ok();
    }

    @DeleteMapping
    public ApiResult<Void> purgeAllTrash() {
        docLibraryFileService.purgeAllTrash();
        return ApiResult.ok();
    }
}
