package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.NbNotebookSaveRequest;
import com.ai.manager.system.domain.entity.NbNotebook;
import com.ai.manager.system.domain.vo.NbTreeNodeVO;
import com.ai.manager.system.service.NbNotebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notebooks")
@RequiredArgsConstructor
public class NbNotebookController {

    private final NbNotebookService nbNotebookService;

    @GetMapping("/tree")
    public ApiResult<List<NbTreeNodeVO>> tree() {
        return ApiResult.ok(nbNotebookService.buildTree());
    }

    @PostMapping
    public ApiResult<NbNotebook> create(@RequestBody NbNotebookSaveRequest request) {
        return ApiResult.ok(nbNotebookService.createNotebook(request));
    }

    @PutMapping("/{id}")
    public ApiResult<NbNotebook> update(@PathVariable Long id, @RequestBody NbNotebookSaveRequest request) {
        return ApiResult.ok(nbNotebookService.updateNotebook(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        nbNotebookService.deleteNotebook(id);
        return ApiResult.ok();
    }
}
