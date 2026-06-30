package com.ai.manager.system.job;

import com.ai.manager.system.service.NoteContentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteContentReconcileJob {

    private final NoteContentSyncService noteContentSyncService;

    /** 每 5 分钟重试待上传云盘的笔记正文 */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcilePendingUploads() {
        noteContentSyncService.scheduleReconcileAll();
    }
}
