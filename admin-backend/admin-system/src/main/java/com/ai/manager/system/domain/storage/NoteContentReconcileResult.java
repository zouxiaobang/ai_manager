package com.ai.manager.system.domain.storage;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NoteContentReconcileResult {

    String content;

    NoteContentMeta meta;

    /** LOCAL | CLOUD */
    String source;

    String syncStatus;

    String syncError;

    boolean needCloudUpload;

    boolean localBackfilled;
}
