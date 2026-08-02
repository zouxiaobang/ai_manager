package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.entity.DocLibraryEventLog;
import com.ai.manager.system.mapper.DocLibraryEventLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library/events")
@RequiredArgsConstructor
public class DocLibraryEventController {

    private final DocLibraryEventLogMapper docLibraryEventLogMapper;

    @PostMapping
    public ApiResult<Void> batchSave(@jakarta.validation.Valid @RequestBody List<DocLibraryEventLog> events) {
        for (DocLibraryEventLog event : events) {
            docLibraryEventLogMapper.insert(event);
        }
        return ApiResult.ok();
    }
}
