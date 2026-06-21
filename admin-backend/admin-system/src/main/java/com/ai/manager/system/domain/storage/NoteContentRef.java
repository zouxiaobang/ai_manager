package com.ai.manager.system.domain.storage;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NoteContentRef {

    Long noteId;

    String storageType;

    String storagePath;

    Long storageFsId;
}
