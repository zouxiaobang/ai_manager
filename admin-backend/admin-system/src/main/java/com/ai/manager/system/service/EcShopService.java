package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcShopSaveRequest;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.vo.EcShopListItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcShopService extends IService<EcShop> {

    PageResult<EcShopListItemVO> pageShops(String keyword, Long platformId, Long page, Long pageSize);

    List<EcShopListItemVO> listShopOptions(Long platformId);

    EcShopListItemVO getShopDetail(Long id);

    EcShopListItemVO createShop(EcShopSaveRequest request);

    EcShopListItemVO updateShop(Long id, EcShopSaveRequest request);

    void deleteShop(Long id);
}
