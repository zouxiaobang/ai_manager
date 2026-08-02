package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.DocLibraryFileBatchMoveRequest;
import com.ai.manager.system.domain.dto.DocLibraryFileMoveRequest;
import com.ai.manager.system.domain.dto.DocLibraryFileRenameRequest;
import com.ai.manager.system.domain.dto.DocLibrarySearchRequest;
import com.ai.manager.system.domain.entity.DocLibraryFile;
import com.ai.manager.system.domain.vo.DocLibraryFileDetailVO;
import com.ai.manager.system.domain.vo.DocLibraryFileVO;
import com.ai.manager.system.domain.vo.DocLibraryStatsVO;
import com.ai.manager.system.domain.vo.DocLibraryTrashItemVO;
import com.ai.manager.system.mapper.DocLibraryFileMapper;
import com.ai.manager.system.service.DocLibraryFileService;
import com.ai.manager.system.service.DocLibraryFolderService;
import com.ai.manager.system.service.DocLibraryTagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocLibraryFileServiceImpl extends ServiceImpl<DocLibraryFileMapper, DocLibraryFile> implements DocLibraryFileService {

    private final DocLibraryFolderService docLibraryFolderService;
    private final DocLibraryTagService docLibraryTagService;

    @Override
    public IPage<DocLibraryFileVO> listFiles(Long folderId, String sort, String order, int page, int size) {
        Page<DocLibraryFile> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DocLibraryFile> wrapper = new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0);
        if (folderId != null) {
            wrapper.eq(DocLibraryFile::getFolderId, folderId);
        }
        if (StringUtils.isNotBlank(sort)) {
            boolean asc = !"desc".equalsIgnoreCase(order);
            if ("name".equals(sort)) {
                wrapper.orderBy(asc, asc, DocLibraryFile::getName);
            } else if ("file_size".equals(sort)) {
                wrapper.orderBy(asc, asc, DocLibraryFile::getFileSize);
            } else {
                wrapper.orderBy(asc, asc, DocLibraryFile::getUpdateTime);
            }
        } else {
            wrapper.orderByDesc(DocLibraryFile::getUpdateTime);
        }
        IPage<DocLibraryFile> result = page(pageParam, wrapper);
        return result.convert(this::toFileVO);
    }

    @Override
    public DocLibraryFileDetailVO getFileDetail(Long id) {
        DocLibraryFile entity = getById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        DocLibraryFileDetailVO vo = new DocLibraryFileDetailVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setExtension(entity.getExtension());
        vo.setFileSize(entity.getFileSize());
        vo.setStoragePath(entity.getStoragePath());
        vo.setFolderId(entity.getFolderId());
        vo.setDescription(entity.getDescription());
        vo.setIsPinned(entity.getIsPinned());
        vo.setViewCount(entity.getViewCount());
        vo.setDownloadCount(entity.getDownloadCount());
        vo.setKbStatus(entity.getKbStatus());
        vo.setTags(docLibraryTagService.getTagsByFileId(id));
        return vo;
    }

    @Override
    public void renameFile(Long id, DocLibraryFileRenameRequest req) {
        DocLibraryFile entity = getById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        entity.setName(req.getName());
        updateById(entity);
    }

    @Override
    public void moveFile(Long id, DocLibraryFileMoveRequest req) {
        DocLibraryFile entity = getById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        entity.setFolderId(req.getFolderId());
        updateById(entity);
    }

    @Override
    @Transactional
    public void batchMove(Long folderId, DocLibraryFileBatchMoveRequest req) {
        List<Long> ids = req.getIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<DocLibraryFile> files = list(new LambdaQueryWrapper<DocLibraryFile>()
                .in(DocLibraryFile::getId, ids)
                .eq(DocLibraryFile::getDeleted, 0));
        for (DocLibraryFile file : files) {
            file.setFolderId(folderId);
        }
        updateBatchById(files);
    }

    @Override
    public void togglePin(Long id) {
        DocLibraryFile entity = getById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        entity.setIsPinned(entity.getIsPinned() == 1 ? 0 : 1);
        updateById(entity);
    }

    @Override
    public void updateDescription(Long id, String description) {
        DocLibraryFile entity = getById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        entity.setDescription(description);
        updateById(entity);
    }

    @Override
    public void softDelete(Long id) {
        DocLibraryFile entity = getById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        entity.setDeleted(1);
        entity.setDeletedAt(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        List<DocLibraryFile> files = list(new LambdaQueryWrapper<DocLibraryFile>()
                .in(DocLibraryFile::getId, ids)
                .eq(DocLibraryFile::getDeleted, 0));
        for (DocLibraryFile file : files) {
            file.setDeleted(1);
            file.setDeletedAt(LocalDateTime.now());
        }
        updateBatchById(files);
    }

    @Override
    public List<DocLibraryTrashItemVO> listTrash() {
        return baseMapper.selectTrashList();
    }

    @Override
    public void restoreFile(Long id) {
        baseMapper.restoreById(id);
    }

    @Override
    public void purgeFile(Long id) {
        Long deletedId = baseMapper.selectDeletedId(id);
        if (deletedId == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        baseMapper.physicalDeleteById(id);
    }

    @Override
    @Transactional
    public void purgeAllTrash() {
        List<DocLibraryFile> trashFiles = list(new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 1));
        for (DocLibraryFile file : trashFiles) {
            baseMapper.physicalDeleteById(file.getId());
        }
    }

    @Override
    public void incrementView(Long id) {
        baseMapper.incrementViewCount(id);
    }

    @Override
    public void incrementDownload(Long id) {
        baseMapper.incrementDownloadCount(id);
    }

    @Override
    public String toggleKbStatus(Long id) {
        DocLibraryFile entity = getById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String current = entity.getKbStatus();
        String next;
        if ("NONE".equals(current) || current == null) {
            next = "PENDING";
        } else if ("PENDING".equals(current)) {
            next = "NONE";
        } else if ("FAILED".equals(current)) {
            next = "PENDING";
        } else if ("READY".equals(current)) {
            next = "NONE";
        } else {
            next = "PENDING";
        }
        entity.setKbStatus(next);
        if (!"PENDING".equals(next)) {
            entity.setKbProcessedAt(LocalDateTime.now());
        }
        updateById(entity);
        return next;
    }

    @Override
    public IPage<DocLibraryFileVO> listKbReadyFiles(int page, int size) {
        Page<DocLibraryFile> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DocLibraryFile> wrapper = new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0)
                .in(DocLibraryFile::getKbStatus, "READY", "PENDING");
        IPage<DocLibraryFile> result = page(pageParam, wrapper);
        return result.convert(this::toFileVO);
    }

    @Override
    public DocLibraryStatsVO getKbStats() {
        DocLibraryStatsVO stats = baseMapper.selectStats();
        long readyCount = count(new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0)
                .eq(DocLibraryFile::getKbStatus, "READY"));
        long processingCount = count(new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0)
                .in(DocLibraryFile::getKbStatus, "PENDING", "PROCESSING"));
        stats.setKbReadyCount(readyCount);
        stats.setKbProcessingCount(processingCount);
        return stats;
    }

    @Override
    public IPage<DocLibraryFileVO> search(DocLibrarySearchRequest req) {
        Page<DocLibraryFile> pageParam = new Page<>(req.getPage(), req.getSize());
        LambdaQueryWrapper<DocLibraryFile> wrapper = new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0);
        if (StringUtils.isNotBlank(req.getKeyword())) {
            wrapper.like(DocLibraryFile::getName, req.getKeyword());
        }
        if (req.getFolderId() != null) {
            wrapper.eq(DocLibraryFile::getFolderId, req.getFolderId());
        }
        wrapper.orderByDesc(DocLibraryFile::getUpdateTime);
        IPage<DocLibraryFile> result = page(pageParam, wrapper);
        return result.convert(this::toFileVO);
    }

    @Override
    public IPage<DocLibraryFileVO> listFavorites(int page, int size) {
        Page<DocLibraryFile> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DocLibraryFile> wrapper = new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0)
                .eq(DocLibraryFile::getIsPinned, 1)
                .orderByDesc(DocLibraryFile::getUpdateTime);
        IPage<DocLibraryFile> result = page(pageParam, wrapper);
        return result.convert(this::toFileVO);
    }

    @Override
    public List<DocLibraryFileVO> listRecent(int limit) {
        List<DocLibraryFile> list = list(new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0)
                .orderByDesc(DocLibraryFile::getUpdateTime)
                .last("limit " + limit));
        return list.stream().map(this::toFileVO).toList();
    }

    @Override
    public DocLibraryStatsVO getStats() {
        DocLibraryStatsVO stats = baseMapper.selectStats();
        long readyCount = count(new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0)
                .eq(DocLibraryFile::getKbStatus, "READY"));
        long processingCount = count(new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getDeleted, 0)
                .in(DocLibraryFile::getKbStatus, "PENDING", "PROCESSING"));
        stats.setKbReadyCount(readyCount);
        stats.setKbProcessingCount(processingCount);
        return stats;
    }

    @Override
    public DocLibraryStatsVO getFolderStats(Long folderId) {
        DocLibraryStatsVO stats = new DocLibraryStatsVO();
        long count = count(new LambdaQueryWrapper<DocLibraryFile>()
                .eq(DocLibraryFile::getFolderId, folderId)
                .eq(DocLibraryFile::getDeleted, 0));
        stats.setTotalFiles(count);
        return stats;
    }

    private DocLibraryFileVO toFileVO(DocLibraryFile entity) {
        DocLibraryFileVO vo = new DocLibraryFileVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setExtension(entity.getExtension());
        vo.setFileSize(entity.getFileSize());
        vo.setFolderId(entity.getFolderId());
        vo.setIsPinned(entity.getIsPinned());
        vo.setViewCount(entity.getViewCount());
        vo.setDownloadCount(entity.getDownloadCount());
        vo.setKbStatus(entity.getKbStatus());
        vo.setTags(docLibraryTagService.getTagsByFileId(entity.getId()));
        return vo;
    }
}
