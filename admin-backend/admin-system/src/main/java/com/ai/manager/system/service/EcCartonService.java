package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcCartonSaveRequest;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.vo.EcCartonBackfillTaskVO;
import com.ai.manager.system.domain.vo.EcCartonCalculateResultVO;
import com.ai.manager.system.domain.vo.EcCartonListItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface EcCartonService extends IService<EcCarton> {

    PageResult<EcCartonListItemVO> pageCartons(String keyword, Long page, Long pageSize);

    EcCartonListItemVO getCartonDetail(Long id);

    EcCartonListItemVO createCarton(EcCartonSaveRequest request);

    EcCartonListItemVO updateCarton(Long id, EcCartonSaveRequest request);

    void deleteCarton(Long id);

    EcCartonListItemVO matchCarton(java.math.BigDecimal lengthCm,
                                   java.math.BigDecimal widthCm,
                                   java.math.BigDecimal heightCm,
                                   Long factoryId);

    /** 为全部 SKU 按单品尺寸回填 carton_id（与 match 规则一致，含旋转匹配） */
    int backfillSkuCartons();

    EcCartonCalculateResultVO calculateCartons(java.math.BigDecimal lengthCm,
                                               java.math.BigDecimal widthCm,
                                               java.math.BigDecimal heightCm,
                                               Long factoryId);

    String startBackfillSkuCartonsAsync();

    EcCartonBackfillTaskVO getBackfillTask(String taskId);
}
