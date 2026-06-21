package com.ai.manager.system.service;

import com.ai.manager.system.domain.vo.EcImageUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface NbNoteImageUploadService {

    EcImageUploadVO upload(MultipartFile file);
}
