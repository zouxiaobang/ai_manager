package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.client.BaiduPanClient;
import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.vo.EcImageUploadVO;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.NbNoteImageUploadService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class NbNoteImageUploadServiceImpl implements NbNoteImageUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final BaiduPanAuthService baiduPanAuthService;
    private final BaiduPanClient baiduPanClient;
    private final BaiduPanProperties baiduPanProperties;
    private final NoteStorageProperties noteStorageProperties;

    @Value("${ai-manager.upload.notebook-images-path:uploads/notebook/images}")
    private String notebookImagesPath;

    private Path uploadDir;
    private volatile boolean panImagesDirEnsured;

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Paths.get(notebookImagesPath).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    @Override
    public EcImageUploadVO upload(MultipartFile file) {
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

        byte[] bytes;
        try {
            Path target = uploadDir.resolve(storedName).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法文件名");
            }
            file.transferTo(target);
            bytes = Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片保存失败");
        }

        syncToBaiduPan(storedName, bytes);
        return new EcImageUploadVO(storedName);
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    private void syncToBaiduPan(String storedName, byte[] bytes) {
        if (!"BAIDU_PAN".equalsIgnoreCase(noteStorageProperties.getType())) {
            return;
        }
        if (!baiduPanAuthService.isAuthorized()) {
            log.warn("百度网盘未授权，笔记本图片仅保存到本地: {}", storedName);
            return;
        }
        try {
            String accessToken = baiduPanAuthService.requireAccessToken();
            ensurePanImagesDir(accessToken);
            String panPath = baiduPanProperties.imagesDir() + "/" + storedName;
            baiduPanClient.upload(accessToken, panPath, bytes);
        } catch (Exception e) {
            log.error("笔记本图片同步到百度网盘失败: {}", storedName, e);
        }
    }

    private void ensurePanImagesDir(String accessToken) throws IOException, InterruptedException {
        if (panImagesDirEnsured) {
            return;
        }
        synchronized (this) {
            if (panImagesDirEnsured) {
                return;
            }
            baiduPanClient.ensureDir(accessToken, baiduPanProperties.rootPath());
            baiduPanClient.ensureDir(accessToken, baiduPanProperties.imagesDir());
            panImagesDirEnsured = true;
        }
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
