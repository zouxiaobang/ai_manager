package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.ImageSpaceRenameRequest;
import com.ai.manager.system.domain.vo.ImageSpaceCategoryNodeVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageDetailVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageItemVO;
import com.ai.manager.system.domain.vo.ImageSpaceNameCheckVO;
import com.ai.manager.system.domain.vo.ImageSpaceNormalizeResultVO;

import java.util.List;

/**
 * 图片空间服务接口
 *
 * <p>提供图片空间的分类列表查询、图片分页查询、图片详情查看、文件名校验、
 * 图片重命名、图片删除、电商图片名称规范化和电商文件名重命名等图片空间管理功能。</p>
 */
public interface ImageSpaceService {

    /**
     * 查询图片分类列表
     *
     * @return 图片分类节点列表
     */
    List<ImageSpaceCategoryNodeVO> listCategories();

    /**
     * 分页查询图片列表
     *
     * @param zone       区域
     * @param categoryId 分类ID
     * @param keyword    关键词
     * @param page       页码
     * @param pageSize   每页条数
     * @return 图片分页结果
     */
    PageResult<ImageSpaceImageItemVO> pageImages(
            String zone,
            String categoryId,
            String keyword,
            Long page,
            Long pageSize
    );

    /**
     * 获取图片详情
     *
     * @param zone         区域
     * @param relativePath 相对路径
     * @return 图片详情信息
     */
    ImageSpaceImageDetailVO getImageDetail(String zone, String relativePath);

    /**
     * 校验文件名是否可用
     *
     * @param zone         区域
     * @param relativePath 相对路径
     * @param newFileName  新文件名
     * @return 文件名校验结果
     */
    ImageSpaceNameCheckVO checkFileName(String zone, String relativePath, String newFileName);

    /**
     * 重命名图片
     *
     * @param request 重命名请求参数
     * @return 重命名后的图片详情
     */
    ImageSpaceImageDetailVO renameImage(ImageSpaceRenameRequest request);

    /**
     * 删除图片
     *
     * @param zone         区域
     * @param relativePath 相对路径
     */
    void deleteImage(String zone, String relativePath);

    /**
     * 规范化电商图片名称
     *
     * @param dryRun 是否试运行
     * @return 规范化结果
     */
    ImageSpaceNormalizeResultVO normalizeEcommerceImageNames(boolean dryRun);

    /**
     * 重命名电商文件名
     *
     * @param oldFileName 旧文件名
     * @param newFileName 新文件名
     * @return 重命名结果
     */
    String renameEcommerceFileName(String oldFileName, String newFileName);
}
