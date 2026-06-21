package com.ai.manager.system.service.storage;

import com.ai.manager.system.domain.storage.NoteContentRef;
import com.ai.manager.system.domain.storage.NoteContentSaveResult;

public interface NoteContentStorage {

    String type();

    NoteContentSaveResult save(NoteContentRef ref, String content);

    String load(NoteContentRef ref);

    void delete(NoteContentRef ref);

    void ensureRoot();
}
