package com.ai.manager.system.domain.storage;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NoteContentSaveResult {

    String storagePath;

    Long storageFsId;

    long contentSize;
}
