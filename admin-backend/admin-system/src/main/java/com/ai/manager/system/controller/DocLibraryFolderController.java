package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.DocLibraryFolderSaveRequest;
import com.ai.manager.system.domain.vo.DocLibraryTreeVO;
import com.ai.manager.system.service.DocLibraryFolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library/folders")
@RequiredArgsConstructor
public class DocLibraryFolderController {

    private final DocLibraryFolderService docLibraryFolderService;

    @GetMapping("/tree")
    public ApiResult<List<DocLibraryTreeVO>> getTree() {
        return ApiResult.ok(docLibraryFolderService.getTree());
    }

    @PostMapping
    public ApiResult<DocLibraryTreeVO> createFolder(@RequestBody DocLibraryFolderSaveRequest request) {
        return ApiResult.ok(docLibraryFolderService.createFolder(request));
    }

    @PutMapping("/{id}")
    public ApiResult<DocLibraryTreeVO> updateFolder(@PathVariable Long id, @RequestBody DocLibraryFolderSaveRequest request) {
        return ApiResult.ok(docLibraryFolderService.updateFolder(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteFolder(@PathVariable Long id) {
        docLibraryFolderService.deleteFolder(id);
        return ApiResult.ok();
    }
}
