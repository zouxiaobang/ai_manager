package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.NbNoteSaveRequest;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.vo.NbNoteDetailVO;
import com.ai.manager.system.domain.vo.NbNoteListMetaVO;
import com.ai.manager.system.domain.vo.NbNoteTrashItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NbNoteService extends IService<NbNote> {

    NbNoteDetailVO getNoteDetail(Long id);

    List<NbNoteDetailVO> listRecent(int limit);

    NbNoteDetailVO createNote(NbNoteSaveRequest request);

    NbNoteDetailVO updateNote(Long id, NbNoteSaveRequest request);

    List<NbNoteListMetaVO> listMetaByIds(List<Long> ids);

    void deleteNote(Long id);

    List<NbNoteTrashItemVO> listTrash();

    void restoreNote(Long id);

    void purgeNote(Long id);
}
