package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.NbTodoSaveRequest;
import com.ai.manager.system.domain.vo.NbTodoItemVO;
import com.ai.manager.system.domain.vo.NbTodoMutationVO;
import com.ai.manager.system.service.NbTodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class NbTodoController {

    private final NbTodoService nbTodoService;

    @GetMapping
    public ApiResult<List<NbTodoItemVO>> list(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean today,
            @RequestParam(required = false) Boolean pinned) {
        return ApiResult.ok(nbTodoService.list(completed, today, pinned));
    }

    @GetMapping("/today")
    public ApiResult<List<NbTodoItemVO>> today() {
        return ApiResult.ok(nbTodoService.listToday());
    }

    @GetMapping("/reminders/due")
    public ApiResult<List<NbTodoItemVO>> dueReminders() {
        return ApiResult.ok(nbTodoService.listDueReminders());
    }

    @PostMapping("/{id}/remind-ack")
    public ApiResult<Void> ackRemind(@PathVariable Long id) {
        nbTodoService.ackRemind(id);
        return ApiResult.ok();
    }

    @PostMapping
    public ApiResult<NbTodoItemVO> create(@RequestBody NbTodoSaveRequest request) {
        return ApiResult.ok(nbTodoService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<NbTodoMutationVO> update(@PathVariable Long id, @RequestBody NbTodoSaveRequest request) {
        return ApiResult.ok(nbTodoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        nbTodoService.delete(id);
        return ApiResult.ok();
    }
}
