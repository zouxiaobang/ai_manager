package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class PomodoroSessionSyncRequest {

    private String phase;

    private String runState;

    private Integer remainingSec;

    private Integer phaseTotalSec;

    private Integer sessionWorkRounds;

    private Long planId;

    /** DEVICE / ADMIN，默认 DEVICE */
    private String source;

    /** true=用户操作抢占控制权；false=仅心跳上报（须已是当前控制方） */
    private Boolean takeControl;

    /** 待确认的下一阶段，与 runState=IDLE、remainingSec=0 配合使用 */
    private String pendingPhase;
}
