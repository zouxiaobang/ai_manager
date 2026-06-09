package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcListingLinkPricingRequest;
import com.ai.manager.system.domain.dto.EcListingLinkSaveRequest;
import com.ai.manager.system.domain.entity.EcListingLink;
import com.ai.manager.system.domain.vo.EcListingLinkDetailVO;
import com.ai.manager.system.domain.vo.EcListingLinkPricingVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface EcListingLinkService extends IService<EcListingLink> {

    PageResult<EcListingLinkDetailVO> pageLinks(String keyword, Long shopId, Long platformId,
                                                Long page, Long pageSize);

    List<EcListingLinkDetailVO> listLinksByProductId(Long productId);

    EcListingLinkDetailVO getLinkDetail(Long id);

    EcListingLinkDetailVO createLink(EcListingLinkSaveRequest request);

    EcListingLinkDetailVO updateLink(Long id, EcListingLinkSaveRequest request);

    EcListingLinkDetailVO copyLink(Long id);

    void deleteLink(Long id);

    EcListingLinkPricingVO calculatePricing(EcListingLinkPricingRequest request);

    int recalculateAllPricing();
}
