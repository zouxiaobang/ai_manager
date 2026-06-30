package com.ai.manager.system.service.impl;

import com.ai.manager.system.service.NbNoteContentService;
import com.ai.manager.system.service.NoteContentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteContentSyncServiceImpl implements NoteContentSyncService {

    private final NbNoteContentService nbNoteContentService;

    private final ConcurrentHashMap<Long, Object> noteLocks = new ConcurrentHashMap<>();
    private final AtomicBoolean reconcileRunning = new AtomicBoolean(false);

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

    @Override
    public void scheduleReconcileAll() {
        if (!reconcileRunning.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始笔记正文差值同步");
                nbNoteContentService.reconcileAll();
                log.info("笔记正文差值同步完成");
            } catch (Exception ex) {
                log.warn("笔记正文差值同步失败: {}", ex.getMessage());
            } finally {
                reconcileRunning.set(false);
            }
        });
    }
}
