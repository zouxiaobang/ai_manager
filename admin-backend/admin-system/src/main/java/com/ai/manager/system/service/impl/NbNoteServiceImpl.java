package com.ai.manager.system.service.impl;



import com.ai.manager.common.exception.BusinessException;

import com.ai.manager.common.result.ResultCode;

import com.ai.manager.system.domain.dto.NbNoteSaveRequest;

import com.ai.manager.system.domain.entity.NbNote;

import com.ai.manager.system.domain.entity.NbNotebook;

import com.ai.manager.system.domain.vo.NbNoteDetailVO;

import com.ai.manager.system.domain.vo.NbNoteListMetaVO;

import com.ai.manager.system.domain.vo.NbNoteTagVO;

import com.ai.manager.system.domain.vo.NbNoteTrashItemVO;

import com.ai.manager.system.mapper.NbNoteMapper;

import com.ai.manager.system.service.NbNoteContentService;

import com.ai.manager.system.service.NbNoteService;

import com.ai.manager.system.service.NbNoteTagService;

import com.ai.manager.system.service.NbNotebookService;

import com.ai.manager.system.service.NoteContentSyncService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;



import java.util.ArrayList;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.Objects;



@Service

@RequiredArgsConstructor

public class NbNoteServiceImpl extends ServiceImpl<NbNoteMapper, NbNote> implements NbNoteService {



    private final NbNotebookService nbNotebookService;

    private final NbNoteTagService nbNoteTagService;

    private final NbNoteContentService nbNoteContentService;

    private final NoteContentSyncService noteContentSyncService;



    @Override

    public NbNoteDetailVO getNoteDetail(Long id) {

        NbNote note = getById(id);

        if (note == null) {

            throw new BusinessException(ResultCode.NOT_FOUND);

        }

        return toDetailVO(note, true);

    }



    @Override

    public List<NbNoteDetailVO> listRecent(int limit) {

        int size = limit <= 0 ? 20 : Math.min(limit, 100);

        List<NbNote> notes = list(new LambdaQueryWrapper<NbNote>()

                .orderByDesc(NbNote::getUpdateTime)

                .last("LIMIT " + size));

        return notes.stream().map(note -> toDetailVO(note, false)).toList();

    }



    @Override

    public List<NbNoteListMetaVO> listMetaByIds(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {

            return List.of();

        }

        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().limit(100).toList();

        if (distinct.isEmpty()) {

            return List.of();

        }

        Map<Long, NbNote> noteMap = new LinkedHashMap<>();

        listByIds(distinct).forEach(note -> noteMap.put(note.getId(), note));

        List<NbNoteListMetaVO> result = new ArrayList<>();

        for (Long id : distinct) {

            NbNote note = noteMap.get(id);

            if (note == null) {

                continue;

            }

            NbNoteListMetaVO vo = new NbNoteListMetaVO();

            vo.setId(note.getId());

            vo.setContentExcerpt(note.getContentExcerpt());

            vo.setContentSize(note.getContentSize());

            vo.setSyncStatus(note.getSyncStatus());

            vo.setCreateTime(note.getCreateTime());

            result.add(vo);

        }

        return result;

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public NbNoteDetailVO createNote(NbNoteSaveRequest request) {

        validateNoteRequest(request, true);

        validateNotebookExists(request.getNotebookId());

        NbNote note = new NbNote();

        applyRequest(note, request, false);

        if (note.getSortOrder() == null) {

            note.setSortOrder(nextSortOrder(request.getNotebookId()));

        }

        save(note);

        nbNoteContentService.prepareNewNote(note);

        if (request.getContent() != null) {

            nbNoteContentService.stageContent(note, request.getContent());

        }

        updateById(note);

        if (request.getContent() != null) {

            noteContentSyncService.scheduleSync(note.getId());

        }

        if (request.getTagIds() != null) {

            nbNoteTagService.syncNoteTags(note.getId(), request.getTagIds());

        }

        return toDetailVO(getById(note.getId()), true);

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public NbNoteDetailVO updateNote(Long id, NbNoteSaveRequest request) {

        NbNote existing = getById(id);

        if (existing == null) {

            throw new BusinessException(ResultCode.NOT_FOUND);

        }

        validateNoteRequest(request, false);

        if (request.getNotebookId() != null) {

            validateNotebookExists(request.getNotebookId());

        }

        applyRequest(existing, request, false);

        if (request.getContent() != null) {

            if (!StringUtils.hasText(existing.getStoragePath())) {

                nbNoteContentService.prepareNewNote(existing);

            }

            nbNoteContentService.stageContent(existing, request.getContent());

        }

        updateById(existing);

        if (request.getContent() != null) {

            noteContentSyncService.scheduleSync(id);

        }

        if (request.getTagIds() != null) {

            nbNoteTagService.syncNoteTags(id, request.getTagIds());

        }

        return toDetailVO(getById(id), true);

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void deleteNote(Long id) {

        NbNote note = getById(id);

        if (note == null) {

            throw new BusinessException(ResultCode.NOT_FOUND);

        }

        removeById(id);

    }



    @Override

    public List<NbNoteTrashItemVO> listTrash() {

        return baseMapper.selectTrashList();

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void restoreNote(Long id) {

        int updated = baseMapper.restoreById(id);

        if (updated == 0) {

            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "笔记不存在或不在回收站");

        }

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void purgeNote(Long id) {

        boolean inTrash = baseMapper.selectTrashList().stream()

                .anyMatch(item -> item.getId().equals(id));

        if (!inTrash) {

            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "笔记不存在或不在回收站");

        }

        NbNote note = baseMapper.selectById(id);

        if (note != null) {

            nbNoteContentService.deleteContent(note);

        }

        nbNoteTagService.syncNoteTags(id, List.of());

        baseMapper.physicalDeleteById(id);

    }



    private NbNoteDetailVO toDetailVO(NbNote note, boolean loadFullContent) {

        NbNoteDetailVO vo = new NbNoteDetailVO();

        vo.setId(note.getId());

        vo.setNotebookId(note.getNotebookId());

        vo.setTitle(note.getTitle());

        if (loadFullContent) {

            vo.setContent(nbNoteContentService.loadContent(note));

        } else {

            vo.setContent(note.getContentExcerpt());

        }

        vo.setContentExcerpt(note.getContentExcerpt());

        vo.setContentSize(note.getContentSize());

        vo.setSyncStatus(note.getSyncStatus());

        vo.setSyncError(note.getSyncError());

        vo.setNoteType(note.getNoteType());

        vo.setIsPinned(note.getIsPinned());

        vo.setIsFavorite(note.getIsFavorite());

        vo.setSortOrder(note.getSortOrder());

        vo.setStatus(note.getStatus());

        vo.setCreateTime(note.getCreateTime());

        vo.setUpdateTime(note.getUpdateTime());

        List<NbNoteTagVO> tags = nbNoteTagService.listTagsByNoteId(note.getId());

        vo.setTags(tags);

        return vo;

    }



    private void applyRequest(NbNote note, NbNoteSaveRequest request, boolean includeContent) {

        if (request.getNotebookId() != null) {

            note.setNotebookId(request.getNotebookId());

        }

        if (request.getTitle() != null) {

            note.setTitle(request.getTitle().trim());

        } else if (note.getTitle() == null) {

            note.setTitle("");

        }

        if (includeContent && request.getContent() != null) {

            // 正文由 NbNoteContentService 处理

        }

        if (StringUtils.hasText(request.getNoteType())) {

            note.setNoteType(request.getNoteType());

        } else if (note.getNoteType() == null) {

            note.setNoteType("NOTE");

        }

        if (request.getPinned() != null) {

            note.setIsPinned(Boolean.TRUE.equals(request.getPinned()) ? 1 : 0);

        } else if (note.getIsPinned() == null) {

            note.setIsPinned(0);

        }

        if (request.getFavorite() != null) {

            note.setIsFavorite(Boolean.TRUE.equals(request.getFavorite()) ? 1 : 0);

        } else if (note.getIsFavorite() == null) {

            note.setIsFavorite(0);

        }

        if (request.getSortOrder() != null) {

            note.setSortOrder(request.getSortOrder());

        }

        if (StringUtils.hasText(request.getStatus())) {

            note.setStatus(request.getStatus());

        } else if (note.getStatus() == null) {

            note.setStatus("PUBLISHED");

        }

    }



    private void validateNoteRequest(NbNoteSaveRequest request, boolean creating) {

        if (request == null) {

            throw new BusinessException(ResultCode.BAD_REQUEST);

        }

        if (creating && request.getTitle() == null) {

            request.setTitle("");

        }

    }



    private void validateNotebookExists(Long notebookId) {

        if (notebookId == null) {

            return;

        }

        NbNotebook notebook = nbNotebookService.getById(notebookId);

        if (notebook == null) {

            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "所属文件夹不存在");

        }

    }



    private int nextSortOrder(Long notebookId) {

        NbNote last = getOne(new LambdaQueryWrapper<NbNote>()

                .eq(notebookId != null, NbNote::getNotebookId, notebookId)

                .isNull(notebookId == null, NbNote::getNotebookId)

                .orderByDesc(NbNote::getSortOrder)

                .last("LIMIT 1"));

        return last == null || last.getSortOrder() == null ? 0 : last.getSortOrder() + 1;

    }

}

