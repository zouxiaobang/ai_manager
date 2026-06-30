package com.ai.manager.system.service;

import com.ai.manager.system.domain.vo.EcImageUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface EcImageUploadService {

    EcImageUploadVO uploadEcommerceImage(MultipartFile file);

    /** 纸箱预览图：按「纸箱名-预览」命名，本地落盘并双写网盘 */
    EcImageUploadVO uploadCartonPreviewImage(MultipartFile file, String cartonName);
}
