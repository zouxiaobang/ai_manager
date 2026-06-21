package com.ai.manager.system.runner;

import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.mapper.NbNoteMapper;
import com.ai.manager.system.service.NbNoteContentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 将历史 nb_note.content 列中的正文迁移到外置存储。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteContentMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final NbNoteMapper nbNoteMapper;
    private final NbNoteContentService nbNoteContentService;

    @Override
    public void run(ApplicationArguments args) {
        if (!hasLegacyContentColumn()) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, content
                FROM nb_note
                WHERE deleted = 0
                  AND content IS NOT NULL
                  AND content <> ''
                  AND (storage_path IS NULL OR storage_path = '')
                LIMIT 200
                """);
        if (rows.isEmpty()) {
            return;
        }
        log.info("开始迁移 {} 条历史笔记正文到外置存储", rows.size());
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String content = (String) row.get("content");
            NbNote note = nbNoteMapper.selectById(id);
            if (note == null) {
                continue;
            }
            try {
                nbNoteContentService.prepareNewNote(note);
                nbNoteContentService.stageContent(note, content);
                nbNoteMapper.updateById(note);
                nbNoteContentService.syncContentToStorage(id);
                jdbcTemplate.update("UPDATE nb_note SET content = NULL WHERE id = ?", id);
                log.info("已迁移笔记正文 noteId={}", id);
            } catch (Exception e) {
                log.error("迁移笔记正文失败 noteId={}", id, e);
            }
        }
    }

    private boolean hasLegacyContentColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'nb_note'
                      AND COLUMN_NAME = 'content'
                    """, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
