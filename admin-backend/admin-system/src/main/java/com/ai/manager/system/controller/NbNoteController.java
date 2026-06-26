package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.NbNoteMetaBatchRequest;
import com.ai.manager.system.domain.dto.NbNoteSaveRequest;
import com.ai.manager.system.domain.vo.NbNoteDetailVO;
import com.ai.manager.system.domain.vo.NbNoteListMetaVO;
import com.ai.manager.system.domain.vo.NbNoteTrashItemVO;
import com.ai.manager.system.service.NbNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NbNoteController {

    private final NbNoteService nbNoteService;

    @GetMapping("/recent")
    public ApiResult<List<NbNoteDetailVO>> recent(@RequestParam(required = false, defaultValue = "20") int limit) {
        return ApiResult.ok(nbNoteService.listRecent(limit));
    }

    @PostMapping("/meta")
    public ApiResult<List<NbNoteListMetaVO>> meta(@RequestBody NbNoteMetaBatchRequest request) {
        List<Long> ids = request == null ? List.of() : request.getIds();
        return ApiResult.ok(nbNoteService.listMetaByIds(ids));
    }

    @GetMapping("/trash")
    public ApiResult<List<NbNoteTrashItemVO>> trash() {
        return ApiResult.ok(nbNoteService.listTrash());
    }

    @GetMapping("/{id}")
    public ApiResult<NbNoteDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(nbNoteService.getNoteDetail(id));
    }

    @PostMapping
    public ApiResult<NbNoteDetailVO> create(@RequestBody NbNoteSaveRequest request) {
        return ApiResult.ok(nbNoteService.createNote(request));
    }

    @PutMapping("/{id}")
    public ApiResult<NbNoteDetailVO> update(@PathVariable Long id, @RequestBody NbNoteSaveRequest request) {
        return ApiResult.ok(nbNoteService.updateNote(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        nbNoteService.deleteNote(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/restore")
    public ApiResult<Void> restore(@PathVariable Long id) {
        nbNoteService.restoreNote(id);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}/purge")
    public ApiResult<Void> purge(@PathVariable Long id) {
        nbNoteService.purgeNote(id);
        return ApiResult.ok();
    }

    @DeleteMapping("/trash")
    public ApiResult<Void> purgeAll() {
        nbNoteService.purgeAllTrash();
        return ApiResult.ok();
    }
}
