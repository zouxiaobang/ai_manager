package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.ImageSpaceRenameRequest;
import com.ai.manager.system.domain.vo.ImageSpaceCategoryNodeVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageDetailVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageItemVO;
import com.ai.manager.system.domain.vo.ImageSpaceNameCheckVO;
import com.ai.manager.system.domain.vo.ImageSpaceNormalizeResultVO;
import com.ai.manager.system.service.ImageSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图片空间控制器
 *
 * <p>所属模块：图片空间模块</p>
 * <p>API路径前缀：/api/image-space</p>
 * <p>功能描述：提供图片分类、图片列表、图片详情、重命名、删除、名称规范化等图片空间管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/image-space")
@RequiredArgsConstructor
public class ImageSpaceController {

    private final ImageSpaceService imageSpaceService;

    /**
     * 获取图片分类列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/image-space/categories</p>
     *
     * @return 图片分类节点列表
     */
    @GetMapping("/categories")
    public ApiResult<List<ImageSpaceCategoryNodeVO>> categories() {
        return ApiResult.ok(imageSpaceService.listCategories());
    }

    /**
     * 分页查询图片列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/image-space/images</p>
     *
     * @param zone 图片区域
     * @param categoryId 分类ID
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页条数
     * @return 图片分页结果
     */
    @GetMapping("/images")
    public ApiResult<PageResult<ImageSpaceImageItemVO>> images(
            @RequestParam(defaultValue = "ECOMMERCE_IMAGES") String zone,
            @RequestParam(defaultValue = "all") String categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return ApiResult.ok(imageSpaceService.pageImages(zone, categoryId, keyword, page, pageSize));
    }

    /**
     * 获取图片详情
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/image-space/images/detail</p>
     *
     * @param zone 图片区域
     * @param relativePath 相对路径
     * @return 图片详情
     */
    @GetMapping("/images/detail")
    public ApiResult<ImageSpaceImageDetailVO> imageDetail(
            @RequestParam String zone,
            @RequestParam String relativePath
    ) {
        return ApiResult.ok(imageSpaceService.getImageDetail(zone, relativePath));
    }

    /**
     * 检查文件名是否可用
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/image-space/images/check-name</p>
     *
     * @param zone 图片区域
     * @param relativePath 相对路径
     * @param newFileName 新文件名
     * @return 名称检查结果
     */
    @GetMapping("/images/check-name")
    public ApiResult<ImageSpaceNameCheckVO> checkName(
            @RequestParam String zone,
            @RequestParam String relativePath,
            @RequestParam String newFileName
    ) {
        return ApiResult.ok(imageSpaceService.checkFileName(zone, relativePath, newFileName));
    }

    /**
     * 重命名图片
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/image-space/images/rename</p>
     *
     * @param request 重命名请求参数
     * @return 重命名后的图片详情
     */
    @PutMapping("/images/rename")
    public ApiResult<ImageSpaceImageDetailVO> rename(@jakarta.validation.Valid @RequestBody ImageSpaceRenameRequest request) {
        return ApiResult.ok(imageSpaceService.renameImage(request));
    }

    /**
     * 删除图片
     *
     * <p>HTTP方法：DELETE</p>
     * <p>路径：/api/image-space/images</p>
     *
     * @param zone 图片区域
     * @param relativePath 相对路径
     * @return 操作结果
     */
    @DeleteMapping("/images")
    public ApiResult<Void> delete(
            @RequestParam String zone,
            @RequestParam String relativePath
    ) {
        imageSpaceService.deleteImage(zone, relativePath);
        return ApiResult.ok();
    }

    /**
     * 规范化电商图片名称
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/image-space/normalize-ecommerce-names</p>
     *
     * @param dryRun 是否试运行
     * @return 规范化结果
     */
    @PostMapping("/normalize-ecommerce-names")
    public ApiResult<ImageSpaceNormalizeResultVO> normalizeEcommerceNames(
            @RequestParam(defaultValue = "true") boolean dryRun
    ) {
        return ApiResult.ok(imageSpaceService.normalizeEcommerceImageNames(dryRun));
    }
}
