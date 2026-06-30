package com.ai.manager.system.service;

import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.storage.NoteContentMeta;
import com.ai.manager.system.domain.storage.NoteContentReconcileResult;
import com.ai.manager.system.domain.storage.NoteContentRef;

public interface NoteContentVersionService {

    /**
     * 按版本比对本地与云盘正文，返回应展示的内容并决定是否回填/补传。
     */
    NoteContentReconcileResult reconcile(NbNote note, NoteContentRef ref);

    void writeMetaDual(NoteContentRef ref, NoteContentMeta meta);
}
