package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.NbNoteTagSaveRequest;
import com.ai.manager.system.domain.entity.NbNoteTag;
import com.ai.manager.system.domain.vo.NbNoteTagVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NbNoteTagService extends IService<NbNoteTag> {

    List<NbNoteTagVO> listAllTags();

    NbNoteTagVO createTag(NbNoteTagSaveRequest request);

    NbNoteTagVO updateTag(Long id, NbNoteTagSaveRequest request);

    void deleteTag(Long id);

    void syncNoteTags(Long noteId, List<Long> tagIds);

    List<NbNoteTagVO> listTagsByNoteId(Long noteId);
}
