package com.ai.manager.system.domain.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteContentMeta {

    private String contentHash;

    private int contentVersion;

    /** ISO-8601 时间戳 */
    private String updatedAt;

    public static NoteContentMeta fromContent(String content, int contentVersion) {
        return fromContent(content, contentVersion, Instant.now());
    }

    public static NoteContentMeta fromContent(String content, int contentVersion, Instant updatedAt) {
        return NoteContentMeta.builder()
                .contentHash(com.ai.manager.system.util.NoteContentUtils.sha256(content))
                .contentVersion(Math.max(contentVersion, 1))
                .updatedAt(updatedAt.toString())
                .build();
    }

    public Instant updatedAtInstant() {
        if (updatedAt == null || updatedAt.isBlank()) {
            return Instant.EPOCH;
        }
        return Instant.parse(updatedAt);
    }
}
