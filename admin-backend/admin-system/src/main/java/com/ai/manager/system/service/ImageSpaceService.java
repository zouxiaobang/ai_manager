package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.ImageSpaceRenameRequest;
import com.ai.manager.system.domain.vo.ImageSpaceCategoryNodeVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageDetailVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageItemVO;
import com.ai.manager.system.domain.vo.ImageSpaceNameCheckVO;
import com.ai.manager.system.domain.vo.ImageSpaceNormalizeResultVO;

import java.util.List;

public interface ImageSpaceService {

    List<ImageSpaceCategoryNodeVO> listCategories();

    PageResult<ImageSpaceImageItemVO> pageImages(
            String zone,
            String categoryId,
            String keyword,
            Long page,
            Long pageSize
    );

    ImageSpaceImageDetailVO getImageDetail(String zone, String relativePath);

    ImageSpaceNameCheckVO checkFileName(String zone, String relativePath, String newFileName);

    ImageSpaceImageDetailVO renameImage(ImageSpaceRenameRequest request);

    void deleteImage(String zone, String relativePath);

    ImageSpaceNormalizeResultVO normalizeEcommerceImageNames(boolean dryRun);

    String renameEcommerceFileName(String oldFileName, String newFileName);
}
