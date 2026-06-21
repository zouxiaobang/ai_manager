package com.ai.manager.system.service.impl;

import com.ai.manager.system.service.NbNoteContentService;
import com.ai.manager.system.service.NoteContentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteContentSyncServiceImpl implements NoteContentSyncService {

    private final NbNoteContentService nbNoteContentService;

    private final ConcurrentHashMap<Long, Object> noteLocks = new ConcurrentHashMap<>();

    @Override
    public void scheduleSync(Long noteId) {
        if (noteId == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            Object lock = noteLocks.computeIfAbsent(noteId, ignored -> new Object());
            synchronized (lock) {
                nbNoteContentService.syncContentToStorage(noteId);
            }
        });
    }
}
