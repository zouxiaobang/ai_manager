package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcFactorySaveRequest;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.vo.EcFactoryStatsVO;
import com.ai.manager.system.service.EcFactoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电商工厂控制器
 *
 * <p>所属模块：电商模块-工厂管理</p>
 * <p>API路径前缀：/api/ecommerce/factories</p>
 * <p>功能描述：提供工厂的增删改查、工厂统计、工厂选项列表等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/factories")
@RequiredArgsConstructor
public class EcFactoryController {

    private final EcFactoryService ecFactoryService;

    /**
     * 分页查询工厂列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/factories</p>
     *
     * @param keyword 关键词，用于搜索工厂名称等
     * @param factoryType 工厂类型
     * @param status 状态
     * @param page 页码
     * @param pageSize 每页条数
     * @return 工厂分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcFactory>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String factoryType,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecFactoryService.pageFactories(keyword, factoryType, status, page, pageSize));
    }

    /**
     * 获取工厂统计信息
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/factories/stats</p>
     *
     * @return 工厂统计信息
     */
    @GetMapping("/stats")
    public ApiResult<EcFactoryStatsVO> stats() {
        return ApiResult.ok(ecFactoryService.getFactoryStats());
    }

    /**
     * 获取工厂选项列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/factories/options</p>
     *
     * @param factoryType 工厂类型
     * @return 工厂选项列表
     */
    @GetMapping("/options")
    public ApiResult<List<EcFactory>> options(@RequestParam(required = false) String factoryType) {
        return ApiResult.ok(ecFactoryService.listFactoryOptions(factoryType));
    }

    /**
     * 获取工厂详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/factories/{id}</p>
     *
     * @param id 工厂ID
     * @return 工厂详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcFactory> get(@PathVariable Long id) {
        EcFactory factory = ecFactoryService.getById(id);
        if (factory == null) {
            return ApiResult.fail(com.ai.manager.common.result.ResultCode.NOT_FOUND);
        }
        return ApiResult.ok(factory);
    }

    /**
     * 创建工厂
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/factories</p>
     *
     * @param request 工厂保存请求参数
     * @return 创建后的工厂信息
     */
    @PostMapping
    public ApiResult<EcFactory> create(@RequestBody EcFactorySaveRequest request) {
        return ApiResult.ok(ecFactoryService.createFactory(request));
    }

    /**
     * 更新工厂
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/factories/{id}</p>
     *
     * @param id 工厂ID
     * @param request 工厂保存请求参数
     * @return 更新后的工厂信息
     */
    @PutMapping("/{id}")
    public ApiResult<EcFactory> update(@PathVariable Long id, @RequestBody EcFactorySaveRequest request) {
        return ApiResult.ok(ecFactoryService.updateFactory(id, request));
    }

    /**
     * 删除工厂
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/factories/{id}</p>
     *
     * @param id 工厂ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecFactoryService.deleteFactory(id);
        return ApiResult.ok();
    }
}
