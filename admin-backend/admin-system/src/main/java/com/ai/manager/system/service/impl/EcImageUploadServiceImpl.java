package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.vo.EcImageUploadVO;
import com.ai.manager.system.service.EcImageUploadService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class EcImageUploadServiceImpl implements EcImageUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    @Value("${ai-manager.upload.ecommerce-path:uploads/ecommerce}")
    private String ecommerceUploadPath;

    private Path uploadDir;

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Paths.get(ecommerceUploadPath).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    @Override
    public EcImageUploadVO uploadEcommerceImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择图片文件");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 JPG、PNG、WebP、GIF 图片");
        }

        String originalName = file.getOriginalFilename();
        String extension = resolveExtension(originalName, contentType);
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path target = uploadDir.resolve(storedName).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法文件名");
            }
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片保存失败");
        }

        return new EcImageUploadVO(storedName);
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    private String resolveExtension(String originalName, String contentType) {
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
            if (ext.matches("\\.(jpg|jpeg|png|webp|gif)")) {
                return ext;
            }
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
