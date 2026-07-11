package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcProductSaveRequest;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.vo.EcProductDetailVO;
import com.ai.manager.system.domain.vo.EcProductListItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 电商商品服务接口
 *
 * <p>提供电商商品的分页查询、详情查看、创建、更新和删除等基础CRUD功能。</p>
 */
public interface EcProductService extends IService<EcProduct> {

    /**
     * 分页查询商品列表
     *
     * @param keyword  关键词（商品名称、SKU编码等）
     * @param page     页码
     * @param pageSize 每页条数
     * @return 商品分页结果
     */
    PageResult<EcProductListItemVO> pageProducts(String keyword, Long page, Long pageSize);

    /**
     * 获取商品详情
     *
     * @param id 商品ID
     * @return 商品详情信息
     */
    EcProductDetailVO getProductDetail(Long id);

    /**
     * 创建商品
     *
     * @param request 商品保存请求参数
     * @return 创建后的商品详情
     */
    EcProductDetailVO createProduct(EcProductSaveRequest request);

    /**
     * 更新商品
     *
     * @param id      商品ID
     * @param request 商品保存请求参数
     * @return 更新后的商品详情
     */
    EcProductDetailVO updateProduct(Long id, EcProductSaveRequest request);

    /**
     * 删除商品
     *
     * @param id 商品ID
     */
    void deleteProduct(Long id);
}
