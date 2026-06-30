package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.EcExpressPriceSaveRequest;
import com.ai.manager.system.domain.entity.EcExpressPrice;
import com.ai.manager.system.domain.vo.EcExpressPriceVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcExpressPriceService extends IService<EcExpressPrice> {

    List<EcExpressPriceVO> listPrices(Long stationId);

    List<String> listRegionNames();

    EcExpressPriceVO createPrice(EcExpressPriceSaveRequest request);

    EcExpressPriceVO updatePrice(Long id, EcExpressPriceSaveRequest request);

    void deletePrice(Long id);
}
