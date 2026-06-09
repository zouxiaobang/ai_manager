package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class PomodoroSessionVO {

    /** WORK / SHORT_BREAK / LONG_BREAK / IDLE */
    private String phase;

    /** IDLE / RUNNING / PAUSED */
    private String runState;

    private Integer remainingSec;

    private Integer phaseTotalSec;

    private Integer sessionWorkRounds;

    private Long planId;

    /** DEVICE / ADMIN（最近一次写入方） */
    private String source;

    /** 当前控制方：DEVICE / ADMIN */
    private String controller;

    /**
     * 阶段结束后待用户确认的下一阶段：WORK / SHORT_BREAK / LONG_BREAK；无则为 null。
     */
    private String pendingPhase;

    /** 最近一次同步时间（毫秒），用于 RUNNING 时推算剩余秒数 */
    private Long syncedAtMs;
}
