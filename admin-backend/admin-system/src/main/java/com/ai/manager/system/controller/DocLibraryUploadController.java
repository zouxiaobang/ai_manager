package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.entity.DocLibraryFile;
import com.ai.manager.system.mapper.DocLibraryFileMapper;
import com.ai.manager.system.service.storage.LocalLibraryFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/library/upload")
@RequiredArgsConstructor
public class DocLibraryUploadController {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"));
    private static final int THUMBNAIL_WIDTH = 300;

    private final DocLibraryFileMapper docLibraryFileMapper;
    private final LocalLibraryFileStorage fileStorage;

    @PostMapping
    public ApiResult<Void> upload(@RequestParam(required = false) Long folderId,
                                  @RequestParam("file") MultipartFile file) {
        processUpload(folderId, file);
        return ApiResult.ok();
    }

    @PostMapping("/batch")
    public ApiResult<Void> uploadBatch(@RequestParam(required = false) Long folderId,
                                       @RequestParam("files") MultipartFile[] files) {
        for (MultipartFile file : files) {
            processUpload(folderId, file);
        }
        return ApiResult.ok();
    }

    private void processUpload(Long folderId, MultipartFile multipartFile) {
        String originalName = multipartFile.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            originalName = "unnamed";
        }
        String extension = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = originalName.substring(dotIdx + 1).toLowerCase();
        }
        String name = dotIdx > 0 ? originalName.substring(0, dotIdx) : originalName;
        String mimeType = multipartFile.getContentType();
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        try {
            byte[] content = multipartFile.getBytes();
            String storagePath = fileStorage.save("library", originalName, content, extension);
            String thumbnailPath = null;
            if (isImage(extension) && !"svg".equals(extension)) {
                thumbnailPath = generateThumbnail(content, extension);
            }
            DocLibraryFile entity = new DocLibraryFile();
            entity.setFolderId(folderId);
            entity.setName(name);
            entity.setOriginalName(originalName);
            entity.setExtension(extension);
            entity.setMimeType(mimeType);
            entity.setFileSize((long) content.length);
            entity.setStorageType("LOCAL");
            entity.setStoragePath(storagePath);
            entity.setThumbnailPath(thumbnailPath);
            entity.setKbStatus("NONE");
            entity.setIsPinned(0);
            entity.setViewCount(0);
            entity.setDownloadCount(0);
            entity.setSortOrder(0);
            entity.setDeleted(0);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            docLibraryFileMapper.insert(entity);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + originalName, e);
        }
    }

    private boolean isImage(String extension) {
        return IMAGE_EXTENSIONS.contains(extension);
    }

    private String generateThumbnail(byte[] content, String extension) throws Exception {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(content));
        if (original == null) {
            return null;
        }
        int width = original.getWidth();
        int height = original.getHeight();
        if (width <= THUMBNAIL_WIDTH) {
            return null;
        }
        int newHeight = (int) ((double) THUMBNAIL_WIDTH / width * height);
        BufferedImage thumbnail = new BufferedImage(THUMBNAIL_WIDTH, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, THUMBNAIL_WIDTH, newHeight, null);
        g2d.dispose();
        String thumbExt = "jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension) || "bmp".equals(extension) ? "png" : "jpg";
        String format = "png".equals(thumbExt) ? "png" : "jpg";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, format, baos);
        String thumbFileName = "thumb_" + System.currentTimeMillis() + "." + thumbExt;
        return fileStorage.save("library/thumbnails", thumbFileName, baos.toByteArray(), thumbExt);
    }
}
