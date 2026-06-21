package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.NbNoteTagSaveRequest;
import com.ai.manager.system.domain.vo.NbNoteTagVO;
import com.ai.manager.system.service.NbNoteTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/note-tags")
@RequiredArgsConstructor
public class NbNoteTagController {

    private final NbNoteTagService nbNoteTagService;

    @GetMapping
    public ApiResult<List<NbNoteTagVO>> list() {
        return ApiResult.ok(nbNoteTagService.listAllTags());
    }

    @PostMapping
    public ApiResult<NbNoteTagVO> create(@RequestBody NbNoteTagSaveRequest request) {
        return ApiResult.ok(nbNoteTagService.createTag(request));
    }

    @PutMapping("/{id}")
    public ApiResult<NbNoteTagVO> update(@PathVariable Long id, @RequestBody NbNoteTagSaveRequest request) {
        return ApiResult.ok(nbNoteTagService.updateTag(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        nbNoteTagService.deleteTag(id);
        return ApiResult.ok();
    }
}
