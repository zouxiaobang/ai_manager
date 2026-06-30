package com.ai.manager.system.service;

import com.ai.manager.system.domain.entity.NbNote;

public interface NbNoteContentService {

    String loadContent(NbNote note);

    /**
     * 快速暂存正文（Redis + 元数据），不等待网盘上传。
     */
    void stageContent(NbNote note, String content);

    /**
     * 后台将暂存正文同步到网盘/本地存储。
     */
    void syncContentToStorage(Long noteId);

    /**
     * 同步保存（迁移等场景使用）。
     */
    void saveContent(NbNote note, String content);

    void deleteContent(NbNote note);

    void prepareNewNote(NbNote note);

    /**
     * 双向差值同步：云盘→本地回填、本地→云盘补传。
     */
    void reconcileAll();

    /**
     * 单条笔记差值同步。
     */
    void reconcileNote(Long noteId);
}
