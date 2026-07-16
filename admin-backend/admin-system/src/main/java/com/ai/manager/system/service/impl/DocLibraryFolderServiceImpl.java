package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.DocLibraryFolderSaveRequest;
import com.ai.manager.system.domain.entity.DocLibraryFolder;
import com.ai.manager.system.domain.vo.DocLibraryTreeVO;
import com.ai.manager.system.mapper.DocLibraryFolderMapper;
import com.ai.manager.system.service.DocLibraryFileService;
import com.ai.manager.system.service.DocLibraryFolderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocLibraryFolderServiceImpl extends ServiceImpl<DocLibraryFolderMapper, DocLibraryFolder> implements DocLibraryFolderService {

    @Lazy
    private DocLibraryFileService docLibraryFileService;

    @Override
    public DocLibraryFolder getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<DocLibraryTreeVO> getTree() {
        List<DocLibraryTreeVO> allNodes = baseMapper.selectTreeList();
        Map<Long, List<DocLibraryTreeVO>> childrenMap = allNodes.stream()
                .collect(Collectors.groupingBy(n -> n.getParentId() == null ? 0L : n.getParentId()));
        List<DocLibraryTreeVO> tree = new ArrayList<>();
        for (DocLibraryTreeVO node : allNodes) {
            if (node.getParentId() == null || node.getParentId() == 0) {
                buildChildren(node, childrenMap);
                tree.add(node);
            }
        }
        return tree;
    }

    private void buildChildren(DocLibraryTreeVO parent, Map<Long, List<DocLibraryTreeVO>> childrenMap) {
        List<DocLibraryTreeVO> children = childrenMap.get(parent.getId());
        if (children != null) {
            parent.setChildren(children);
            for (DocLibraryTreeVO child : children) {
                buildChildren(child, childrenMap);
            }
        }
    }

    @Override
    public DocLibraryTreeVO createFolder(DocLibraryFolderSaveRequest req) {
        DocLibraryFolder entity = new DocLibraryFolder();
        entity.setParentId(req.getParentId());
        entity.setName(req.getName());
        entity.setIcon(req.getIcon());
        entity.setColor(req.getColor());
        entity.setSortOrder(req.getSortOrder());
        save(entity);
        return toTreeVO(entity);
    }

    @Override
    public DocLibraryTreeVO updateFolder(Long id, DocLibraryFolderSaveRequest req) {
        DocLibraryFolder entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        entity.setParentId(req.getParentId());
        entity.setName(req.getName());
        entity.setIcon(req.getIcon());
        entity.setColor(req.getColor());
        entity.setSortOrder(req.getSortOrder());
        updateById(entity);
        return toTreeVO(entity);
    }

    @Override
    @Transactional
    public void deleteFolder(Long id) {
        DocLibraryFolder entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<Long> subFolderIds = new ArrayList<>();
        collectSubFolderIds(id, subFolderIds);
        subFolderIds.add(id);
        for (Long folderId : subFolderIds) {
            List<Long> fileIds = baseMapper.selectFileIdsByFolderId(folderId);
            if (fileIds != null && !fileIds.isEmpty()) {
                docLibraryFileService.batchDelete(fileIds);
            }
        }
        remove(new LambdaQueryWrapper<DocLibraryFolder>().in(DocLibraryFolder::getId, subFolderIds));
    }

    private void collectSubFolderIds(Long parentId, List<Long> result) {
        List<DocLibraryFolder> children = list(new LambdaQueryWrapper<DocLibraryFolder>()
                .eq(DocLibraryFolder::getParentId, parentId));
        for (DocLibraryFolder child : children) {
            result.add(child.getId());
            collectSubFolderIds(child.getId(), result);
        }
    }

    private DocLibraryTreeVO toTreeVO(DocLibraryFolder entity) {
        DocLibraryTreeVO vo = new DocLibraryTreeVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setName(entity.getName());
        vo.setIcon(entity.getIcon());
        vo.setColor(entity.getColor());
        vo.setSortOrder(entity.getSortOrder());
        return vo;
    }
}
