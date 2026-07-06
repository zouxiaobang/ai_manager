package com.ai.manager.system.domain.vo;

import lombok.Data;

/**
 * 库存 SPU 状态计数（用于前端筛选标签）
 */
@Data
public class EcInventorySpuStatusVO {

    /** SPU 总数 */
    private int total;

    /** 正常 SPU 数 */
    private int normal;

    /** 不足 SPU 数 */
    private int low;

    /** 缺货 SPU 数 */
    private int zero;
}
