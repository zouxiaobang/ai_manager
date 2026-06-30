package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcExpressStationSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.vo.EcExpressStationDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcExpressStationService extends IService<EcExpressStation> {

    PageResult<EcExpressStationDetailVO> pageStations(String keyword, Boolean defaultOnly, List<String> regionNames,
                                                      Long page, Long pageSize);

    EcExpressStationDetailVO getStationDetail(Long id);

    EcExpressStationDetailVO createStation(EcExpressStationSaveRequest request);

    EcExpressStationDetailVO updateStation(Long id, EcExpressStationSaveRequest request);

    EcExpressStationDetailVO copyStation(Long id);

    void deleteStation(Long id);
}
