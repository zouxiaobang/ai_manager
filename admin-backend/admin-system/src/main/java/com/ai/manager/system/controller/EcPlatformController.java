package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcPlatformSaveRequest;
import com.ai.manager.system.domain.vo.EcPlatformListItemVO;
import com.ai.manager.system.service.EcPlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电商平台控制器
 *
 * <p>所属模块：电商模块-平台管理</p>
 * <p>API路径前缀：/api/ecommerce/platforms</p>
 * <p>功能描述：提供电商平台的增删改查、平台选项列表等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/platforms")
@RequiredArgsConstructor
public class EcPlatformController {

    private final EcPlatformService ecPlatformService;

    /**
     * 分页查询平台列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/platforms</p>
     *
     * @param keyword 关键词，用于搜索平台名称等
     * @param channelType 渠道类型
     * @param page 页码
     * @param pageSize 每页条数
     * @return 平台分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcPlatformListItemVO>> list(@RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String channelType,
                                                              @RequestParam(required = false) Long page,
                                                              @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecPlatformService.pagePlatforms(keyword, channelType, page, pageSize));
    }

    /**
     * 获取平台选项列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/platforms/options</p>
     *
     * @return 平台选项列表
     */
    @GetMapping("/options")
    public ApiResult<List<EcPlatformListItemVO>> options() {
        return ApiResult.ok(ecPlatformService.listPlatformOptions());
    }

    /**
     * 获取平台详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/platforms/{id}</p>
     *
     * @param id 平台ID
     * @return 平台详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcPlatformListItemVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecPlatformService.getPlatformDetail(id));
    }

    /**
     * 创建平台
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/platforms</p>
     *
     * @param request 平台保存请求参数
     * @return 创建后的平台信息
     */
    @PostMapping
    public ApiResult<EcPlatformListItemVO> create(@RequestBody EcPlatformSaveRequest request) {
        return ApiResult.ok(ecPlatformService.createPlatform(request));
    }

    /**
     * 更新平台
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/platforms/{id}</p>
     *
     * @param id 平台ID
     * @param request 平台保存请求参数
     * @return 更新后的平台信息
     */
    @PutMapping("/{id}")
    public ApiResult<EcPlatformListItemVO> update(@PathVariable Long id,
                                                   @RequestBody EcPlatformSaveRequest request) {
        return ApiResult.ok(ecPlatformService.updatePlatform(id, request));
    }

    /**
     * 删除平台
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/platforms/{id}</p>
     *
     * @param id 平台ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecPlatformService.deletePlatform(id);
        return ApiResult.ok();
    }
}
