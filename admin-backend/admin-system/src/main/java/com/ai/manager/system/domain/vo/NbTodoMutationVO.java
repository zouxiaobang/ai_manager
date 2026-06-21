package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class NbTodoMutationVO {

    private NbTodoItemVO item;

    /** 完成重复任务后自动生成的一条 */
    private NbTodoItemVO nextOccurrence;
}
