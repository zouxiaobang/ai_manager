package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.DocLibraryFileBatchMoveRequest;
import com.ai.manager.system.domain.dto.DocLibraryFileMoveRequest;
import com.ai.manager.system.domain.dto.DocLibraryFileRenameRequest;
import com.ai.manager.system.domain.entity.DocLibraryFile;
import com.ai.manager.system.domain.vo.DocLibraryFileDetailVO;
import com.ai.manager.system.domain.vo.DocLibraryFileVO;
import com.ai.manager.system.mapper.DocLibraryFileMapper;
import com.ai.manager.system.service.DocLibraryFileService;
import com.ai.manager.system.service.storage.LocalLibraryFileStorage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/library/files")
@RequiredArgsConstructor
public class DocLibraryFileController {

    private final DocLibraryFileService docLibraryFileService;
    private final DocLibraryFileMapper docLibraryFileMapper;
    private final LocalLibraryFileStorage fileStorage;

    @GetMapping
    public ApiResult<IPage<DocLibraryFileVO>> listFiles(
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (Boolean.TRUE.equals(favorite)) {
            return ApiResult.ok(docLibraryFileService.listFavorites(page, size));
        }
        return ApiResult.ok(docLibraryFileService.listFiles(folderId, sort, order, page, size));
    }

    @GetMapping("/recent")
    public ApiResult<List<DocLibraryFileVO>> recent(@RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(docLibraryFileService.listRecent(limit));
    }

    @GetMapping("/favorites")
    public ApiResult<IPage<DocLibraryFileVO>> favorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResult.ok(docLibraryFileService.listFavorites(page, size));
    }

    @GetMapping("/search")
    public ApiResult<IPage<DocLibraryFileVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        com.ai.manager.system.domain.dto.DocLibrarySearchRequest req = new com.ai.manager.system.domain.dto.DocLibrarySearchRequest();
        req.setKeyword(keyword);
        req.setFolderId(folderId);
        req.setPage(page);
        req.setSize(size);
        return ApiResult.ok(docLibraryFileService.search(req));
    }

    @GetMapping("/{id}")
    public ApiResult<DocLibraryFileDetailVO> getFileDetail(@PathVariable Long id) {
        return ApiResult.ok(docLibraryFileService.getFileDetail(id));
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        DocLibraryFile entity = docLibraryFileMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            response.setStatus(404);
            return;
        }
        docLibraryFileService.incrementDownload(id);
        byte[] content = fileStorage.load(entity.getStoragePath());
        if (content == null) {
            response.setStatus(404);
            return;
        }
        String encodedName = UriUtils.encode(
                entity.getOriginalName() != null ? entity.getOriginalName() : entity.getName() + "." + entity.getExtension(),
                StandardCharsets.UTF_8);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(content.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(content);
            os.flush();
        }
    }

    @GetMapping("/{id}/preview")
    public void preview(@PathVariable Long id, HttpServletResponse response) throws IOException {
        DocLibraryFile entity = docLibraryFileMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            response.setStatus(404);
            return;
        }
        docLibraryFileService.incrementView(id);
        String previewPath = entity.getThumbnailPath();
        if (!StringUtils.hasText(previewPath)) {
            previewPath = entity.getStoragePath();
        }
        byte[] content = fileStorage.load(previewPath);
        if (content == null) {
            response.setStatus(404);
            return;
        }
        String contentType = entity.getMimeType();
        if ("svg".equals(entity.getExtension())) {
            contentType = "image/svg+xml";
        } else if ("pdf".equals(entity.getExtension())) {
            contentType = "application/pdf";
        } else if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setContentLengthLong(content.length);
        response.setHeader("Cache-Control", "public, max-age=3600");
        try (OutputStream os = response.getOutputStream()) {
            os.write(content);
            os.flush();
        }
    }

    @PutMapping("/{id}/rename")
    public ApiResult<Void> renameFile(@PathVariable Long id, @RequestBody DocLibraryFileRenameRequest request) {
        docLibraryFileService.renameFile(id, request);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/move")
    public ApiResult<Void> moveFile(@PathVariable Long id, @RequestBody DocLibraryFileMoveRequest request) {
        docLibraryFileService.moveFile(id, request);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/pin")
    public ApiResult<Void> togglePin(@PathVariable Long id) {
        docLibraryFileService.togglePin(id);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/description")
    public ApiResult<Void> updateDescription(@PathVariable Long id, @RequestBody Map<String, String> body) {
        docLibraryFileService.updateDescription(id, body.get("description"));
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> softDelete(@PathVariable Long id) {
        docLibraryFileService.softDelete(id);
        return ApiResult.ok();
    }

    @PostMapping("/batch/delete")
    public ApiResult<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        docLibraryFileService.batchDelete(body.get("ids"));
        return ApiResult.ok();
    }

    @PostMapping("/batch/move")
    public ApiResult<Void> batchMove(@RequestBody DocLibraryFileBatchMoveRequest request) {
        docLibraryFileService.batchMove(request.getFolderId(), request);
        return ApiResult.ok();
    }

    @PostMapping("/batch/download")
    public void batchDownload(@RequestBody Map<String, List<Long>> body, HttpServletResponse response) throws IOException {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            response.setStatus(400);
            return;
        }
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=library-batch.zip");
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Long id : ids) {
                DocLibraryFile entity = docLibraryFileMapper.selectById(id);
                if (entity == null || entity.getDeleted() == 1) continue;
                byte[] content = fileStorage.load(entity.getStoragePath());
                if (content == null) continue;
                String entryName = entity.getOriginalName();
                if (!StringUtils.hasText(entryName)) {
                    entryName = entity.getName() + "." + entity.getExtension();
                }
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(content);
                zos.closeEntry();
                docLibraryFileService.incrementDownload(id);
            }
        }
    }
}
