package com.ai.manager.system.runner;

import com.ai.manager.system.service.NoteContentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteContentReconcileRunner {

    private final NoteContentSyncService noteContentSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        noteContentSyncService.scheduleReconcileAll();
    }
}
