package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcProductSaveRequest;
import com.ai.manager.system.domain.vo.EcProductDetailVO;
import com.ai.manager.system.domain.vo.EcProductListItemVO;
import com.ai.manager.system.service.EcProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 电商商品控制器
 *
 * <p>所属模块：电商模块-商品管理</p>
 * <p>API路径前缀：/api/ecommerce/products</p>
 * <p>功能描述：提供商品的增删改查等基础管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/products")
@RequiredArgsConstructor
public class EcProductController {

    private final EcProductService ecProductService;

    /**
     * 分页查询商品列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/products</p>
     *
     * @param keyword 关键词，用于搜索商品名称、SKU等
     * @param page 页码
     * @param pageSize 每页条数
     * @return 商品分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcProductListItemVO>> list(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Long page,
                                                           @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecProductService.pageProducts(keyword, page, pageSize));
    }

    /**
     * 获取商品详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/products/{id}</p>
     *
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcProductDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecProductService.getProductDetail(id));
    }

    /**
     * 创建商品
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/products</p>
     *
     * @param request 商品保存请求参数
     * @return 创建后的商品详情
     */
    @PostMapping
    public ApiResult<EcProductDetailVO> create(@RequestBody EcProductSaveRequest request) {
        return ApiResult.ok(ecProductService.createProduct(request));
    }

    /**
     * 更新商品
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/products/{id}</p>
     *
     * @param id 商品ID
     * @param request 商品保存请求参数
     * @return 更新后的商品详情
     */
    @PutMapping("/{id}")
    public ApiResult<EcProductDetailVO> update(@PathVariable Long id, @RequestBody EcProductSaveRequest request) {
        return ApiResult.ok(ecProductService.updateProduct(id, request));
    }

    /**
     * 删除商品
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/products/{id}</p>
     *
     * @param id 商品ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecProductService.deleteProduct(id);
        return ApiResult.ok();
    }
}
