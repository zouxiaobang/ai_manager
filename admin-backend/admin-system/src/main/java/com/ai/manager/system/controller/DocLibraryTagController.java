package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.DocLibraryTagSaveRequest;
import com.ai.manager.system.domain.vo.DocLibraryTagVO;
import com.ai.manager.system.service.DocLibraryTagService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/library/tags")
@RequiredArgsConstructor
public class DocLibraryTagController {

    private final DocLibraryTagService docLibraryTagService;

    @GetMapping
    public ApiResult<List<DocLibraryTagVO>> listAll() {
        return ApiResult.ok(docLibraryTagService.listAll());
    }

    @PostMapping
    public ApiResult<DocLibraryTagVO> createTag(@RequestBody DocLibraryTagSaveRequest request) {
        return ApiResult.ok(docLibraryTagService.createTag(request));
    }

    @PutMapping("/{id}")
    public ApiResult<DocLibraryTagVO> updateTag(@PathVariable Long id, @RequestBody DocLibraryTagSaveRequest request) {
        return ApiResult.ok(docLibraryTagService.updateTag(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteTag(@PathVariable Long id) {
        docLibraryTagService.deleteTag(id);
        return ApiResult.ok();
    }

    @PostMapping("/file/{fileId}/tags")
    public ApiResult<Void> syncFileTags(@PathVariable Long fileId, @RequestBody Map<String, List<Long>> body) {
        docLibraryTagService.syncFileTags(fileId, body.get("tagIds"));
        return ApiResult.ok();
    }
}
