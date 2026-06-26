package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.NbTodoSaveRequest;
import com.ai.manager.system.domain.vo.NbTodoItemVO;
import com.ai.manager.system.domain.vo.NbTodoMutationVO;

import java.util.List;

public interface NbTodoService {

    List<NbTodoItemVO> list(Boolean completed, Boolean today, Boolean pinned);

    List<NbTodoItemVO> listToday();

    List<NbTodoItemVO> listDueReminders();

    NbTodoItemVO create(NbTodoSaveRequest request);

    NbTodoMutationVO update(Long id, NbTodoSaveRequest request);

    void delete(Long id);

    void ackRemind(Long id);
}
