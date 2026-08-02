package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.EcExpressPriceSaveRequest;
import com.ai.manager.system.domain.vo.EcExpressPriceVO;
import com.ai.manager.system.service.EcExpressPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电商快递价格控制器
 *
 * <p>所属模块：电商模块-快递管理</p>
 * <p>API路径前缀：/api/ecommerce/express/prices</p>
 * <p>功能描述：提供快递价格的增删改查等快递价格管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/express/prices")
@RequiredArgsConstructor
public class EcExpressPriceController {

    private final EcExpressPriceService ecExpressPriceService;

    /**
     * 获取快递价格列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/express/prices</p>
     *
     * @param stationId 快递站点ID
     * @return 快递价格列表
     */
    @GetMapping
    public ApiResult<List<EcExpressPriceVO>> list(@RequestParam Long stationId) {
        return ApiResult.ok(ecExpressPriceService.listPrices(stationId));
    }

    /**
     * 创建快递价格
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/express/prices</p>
     *
     * @param request 快递价格保存请求参数
     * @return 创建后的快递价格信息
     */
    @PostMapping
    public ApiResult<EcExpressPriceVO> create(@jakarta.validation.Valid @RequestBody EcExpressPriceSaveRequest request) {
        return ApiResult.ok(ecExpressPriceService.createPrice(request));
    }

    /**
     * 更新快递价格
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/express/prices/{id}</p>
     *
     * @param id 价格ID
     * @param request 快递价格保存请求参数
     * @return 更新后的快递价格信息
     */
    @PutMapping("/{id}")
    public ApiResult<EcExpressPriceVO> update(@PathVariable Long id,
                                              @jakarta.validation.Valid @RequestBody EcExpressPriceSaveRequest request) {
        return ApiResult.ok(ecExpressPriceService.updatePrice(id, request));
    }

    /**
     * 删除快递价格
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/express/prices/{id}</p>
     *
     * @param id 价格ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecExpressPriceService.deletePrice(id);
        return ApiResult.ok();
    }
}
