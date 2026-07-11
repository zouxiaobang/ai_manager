package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcCartonSaveRequest;
import com.ai.manager.system.domain.vo.EcCartonBackfillTaskVO;
import com.ai.manager.system.domain.vo.EcCartonCalculateResultVO;
import com.ai.manager.system.domain.vo.EcCartonListItemVO;
import com.ai.manager.system.service.EcCartonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 电商纸箱控制器
 *
 * <p>所属模块：电商模块-纸箱管理</p>
 * <p>API路径前缀：/api/ecommerce/cartons</p>
 * <p>功能描述：提供纸箱的增删改查、纸箱计算匹配、SKU纸箱回填等功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/ecommerce/cartons")
@RequiredArgsConstructor
public class EcCartonController {

    private final EcCartonService ecCartonService;

    /**
     * 分页查询纸箱列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/cartons</p>
     *
     * @param keyword 关键词，用于搜索纸箱名称等
     * @param page 页码
     * @param pageSize 每页条数
     * @return 纸箱分页结果
     */
    @GetMapping
    public ApiResult<PageResult<EcCartonListItemVO>> list(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Long page,
                                                          @RequestParam(required = false) Long pageSize) {
        return ApiResult.ok(ecCartonService.pageCartons(keyword, page, pageSize));
    }

    /**
     * 回填SKU纸箱信息
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/cartons/backfill-sku-cartons</p>
     *
     * @return 回填数量
     */
    @PostMapping("/backfill-sku-cartons")
    public ApiResult<Integer> backfillSkuCartons() {
        return ApiResult.ok(ecCartonService.backfillSkuCartons());
    }

    /**
     * 异步启动SKU纸箱回填任务
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/cartons/backfill-sku-cartons/async</p>
     *
     * @return 任务ID
     */
    @PostMapping("/backfill-sku-cartons/async")
    public ApiResult<String> startBackfillSkuCartonsAsync() {
        return ApiResult.ok(ecCartonService.startBackfillSkuCartonsAsync());
    }

    /**
     * 获取回填任务状态
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/cartons/backfill-sku-cartons/tasks/{taskId}</p>
     *
     * @param taskId 任务ID
     * @return 回填任务信息
     */
    @GetMapping("/backfill-sku-cartons/tasks/{taskId}")
    public ApiResult<EcCartonBackfillTaskVO> getBackfillTask(@PathVariable String taskId) {
        return ApiResult.ok(ecCartonService.getBackfillTask(taskId));
    }

    /**
     * 计算合适的纸箱
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/cartons/calculate</p>
     *
     * @param lengthCm 长度（厘米）
     * @param widthCm 宽度（厘米）
     * @param heightCm 高度（厘米）
     * @param factoryId 工厂ID
     * @return 纸箱计算结果
     */
    @GetMapping("/calculate")
    public ApiResult<EcCartonCalculateResultVO> calculate(@RequestParam java.math.BigDecimal lengthCm,
                                                          @RequestParam java.math.BigDecimal widthCm,
                                                          @RequestParam java.math.BigDecimal heightCm,
                                                          @RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecCartonService.calculateCartons(lengthCm, widthCm, heightCm, factoryId));
    }

    /**
     * 匹配合适的纸箱
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/cartons/match</p>
     *
     * @param lengthCm 长度（厘米）
     * @param widthCm 宽度（厘米）
     * @param heightCm 高度（厘米）
     * @param factoryId 工厂ID
     * @return 匹配的纸箱信息
     */
    @GetMapping("/match")
    public ApiResult<EcCartonListItemVO> match(@RequestParam java.math.BigDecimal lengthCm,
                                               @RequestParam java.math.BigDecimal widthCm,
                                               @RequestParam java.math.BigDecimal heightCm,
                                               @RequestParam(required = false) Long factoryId) {
        return ApiResult.ok(ecCartonService.matchCarton(lengthCm, widthCm, heightCm, factoryId));
    }

    /**
     * 获取纸箱详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/ecommerce/cartons/{id}</p>
     *
     * @param id 纸箱ID
     * @return 纸箱详情
     */
    @GetMapping("/{id}")
    public ApiResult<EcCartonListItemVO> get(@PathVariable Long id) {
        return ApiResult.ok(ecCartonService.getCartonDetail(id));
    }

    /**
     * 创建纸箱
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/ecommerce/cartons</p>
     *
     * @param request 纸箱保存请求参数
     * @return 创建后的纸箱信息
     */
    @PostMapping
    public ApiResult<EcCartonListItemVO> create(@RequestBody EcCartonSaveRequest request) {
        return ApiResult.ok(ecCartonService.createCarton(request));
    }

    /**
     * 更新纸箱
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/cartons/{id}</p>
     *
     * @param id 纸箱ID
     * @param request 纸箱保存请求参数
     * @return 更新后的纸箱信息
     */
    @PutMapping("/{id}")
    public ApiResult<EcCartonListItemVO> update(@PathVariable Long id, @RequestBody EcCartonSaveRequest request) {
        return ApiResult.ok(ecCartonService.updateCarton(id, request));
    }

    /**
     * 上传纸箱预览图片
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/ecommerce/cartons/{id}/preview-image</p>
     *
     * @param id 纸箱ID
     * @param file 图片文件
     * @param cartonName 纸箱名称
     * @return 更新后的纸箱信息
     */
    @PutMapping("/{id}/preview-image")
    public ApiResult<EcCartonListItemVO> uploadPreviewImage(@PathVariable Long id,
                                                            @RequestParam("file") MultipartFile file,
                                                            @RequestParam(value = "cartonName", required = false) String cartonName) {
        return ApiResult.ok(ecCartonService.updateCartonPreviewImage(id, file, cartonName));
    }

    /**
     * 删除纸箱
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/ecommerce/cartons/{id}</p>
     *
     * @param id 纸箱ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        ecCartonService.deleteCarton(id);
        return ApiResult.ok();
    }
}
