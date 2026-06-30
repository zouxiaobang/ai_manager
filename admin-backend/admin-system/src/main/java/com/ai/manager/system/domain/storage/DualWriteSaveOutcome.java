package com.ai.manager.system.domain.storage;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DualWriteSaveOutcome {

    boolean localSaved;

    boolean cloudSaved;

    String localPath;

    String cloudPath;

    Long cloudFsId;

    long contentSize;

    String cloudError;

    public static DualWriteSaveOutcome localOnly(String localPath, long contentSize, String cloudError) {
        return DualWriteSaveOutcome.builder()
                .localSaved(true)
                .cloudSaved(false)
                .localPath(localPath)
                .cloudPath(null)
                .cloudFsId(null)
                .contentSize(contentSize)
                .cloudError(cloudError)
                .build();
    }

    public static DualWriteSaveOutcome dual(String localPath, String cloudPath, Long cloudFsId, long contentSize) {
        return DualWriteSaveOutcome.builder()
                .localSaved(true)
                .cloudSaved(true)
                .localPath(localPath)
                .cloudPath(cloudPath)
                .cloudFsId(cloudFsId)
                .contentSize(contentSize)
                .cloudError(null)
                .build();
    }
}
