package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcProductSaveRequest;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.vo.EcProductDetailVO;
import com.ai.manager.system.domain.vo.EcProductListItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface EcProductService extends IService<EcProduct> {

    PageResult<EcProductListItemVO> pageProducts(String keyword, Long page, Long pageSize);

    EcProductDetailVO getProductDetail(Long id);

    EcProductDetailVO createProduct(EcProductSaveRequest request);

    EcProductDetailVO updateProduct(Long id, EcProductSaveRequest request);

    void deleteProduct(Long id);
}
