package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcExpressStationSaveRequest;
import com.ai.manager.system.domain.vo.EcExpressStationDetailVO;
import com.ai.manager.system.service.EcExpressPriceService;
import com.ai.manager.system.service.EcExpressStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 电商快递站点控制器
 *
 * <p>所属模块：电商模块-快递管理</p>
 * <p>API路径前缀：/api/ecommerce/express/stations</p>
 * <p>功能描述：提供快递站点的增删改查、站点复制、区域列表等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/express/stations")
@RequiredArgsConstructor
public class EcExpressStationController {

    private final EcExpressStationService ecExpressStationService;
    private final EcExpressPriceService ecExpressPriceService;

    /**
     * 分页查询快递站点列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/express/stations</p>
     *
     * @param keyword 关键词，用于搜索站点名称等
     * @param defaultOnly 是否仅显示默认站点
     * @param regionNames 区域名称（逗号分隔）
     * @param page 页码
     * @param pageSize 每页条数
     * @return 快递站点分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcExpressStationDetailVO>> list(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) Boolean defaultOnly,
                                                                @RequestParam(required = false) String regionNames,
                                                                @RequestParam(required = false) Long page,
                                                                @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecExpressStationService.pageStations(
                keyword, defaultOnly, parseRegionNames(regionNames), page, pageSize));
    }

    /**
     * 获取区域列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/express/stations/regions</p>
     *
     * @return 区域名称列表
     */
    @GetMapping("/regions")
    public ApiResult<List<String>> listRegions() {
        return ApiResult.ok(ecExpressPriceService.listRegionNames());
    }

    /**
     * 获取快递站点详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/express/stations/{id}</p>
     *
     * @param id 站点ID
     * @return 快递站点详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcExpressStationDetailVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecExpressStationService.getStationDetail(id));
    }

    /**
     * 创建快递站点
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/express/stations</p>
     *
     * @param request 快递站点保存请求参数
     * @return 创建后的快递站点详情
     */
    @PostMapping
    public ApiResult<EcExpressStationDetailVO> create(@RequestBody EcExpressStationSaveRequest request) {
        return ApiResult.ok(ecExpressStationService.createStation(request));
    }

    /**
     * 更新快递站点
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/express/stations/{id}</p>
     *
     * @param id 站点ID
     * @param request 快递站点保存请求参数
     * @return 更新后的快递站点详情
     */
    @PutMapping("/{id}")
    public ApiResult<EcExpressStationDetailVO> update(@PathVariable Long id,
                                                      @RequestBody EcExpressStationSaveRequest request) {
        return ApiResult.ok(ecExpressStationService.updateStation(id, request));
    }

    /**
     * 复制快递站点
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/express/stations/{id}/copy</p>
     *
     * @param id 站点ID
     * @return 复制后的快递站点详情
     */
    @PostMapping("/{id}/copy")
    public ApiResult<EcExpressStationDetailVO> copy(@PathVariable Long id) {
        return ApiResult.ok(ecExpressStationService.copyStation(id));
    }

    /**
     * 删除快递站点
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/express/stations/{id}</p>
     *
     * @param id 站点ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecExpressStationService.deleteStation(id);
        return ApiResult.ok();
    }

    private List<String> parseRegionNames(String regionNames) {
        if (regionNames == null || regionNames.isBlank()) {
            return List.of();
        }
        return Arrays.stream(regionNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}
