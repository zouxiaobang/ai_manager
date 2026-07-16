package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.DocLibraryTagSaveRequest;
import com.ai.manager.system.domain.entity.DocLibraryFileTag;
import com.ai.manager.system.domain.entity.DocLibraryTag;
import com.ai.manager.system.domain.vo.DocLibraryTagVO;
import com.ai.manager.system.mapper.DocLibraryFileTagMapper;
import com.ai.manager.system.mapper.DocLibraryTagMapper;
import com.ai.manager.system.service.DocLibraryTagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocLibraryTagServiceImpl extends ServiceImpl<DocLibraryTagMapper, DocLibraryTag> implements DocLibraryTagService {

    private final DocLibraryFileTagMapper docLibraryFileTagMapper;

    @Override
    public List<DocLibraryTagVO> listAll() {
        return list(new LambdaQueryWrapper<DocLibraryTag>()
                .orderByAsc(DocLibraryTag::getName))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public DocLibraryTagVO createTag(DocLibraryTagSaveRequest req) {
        long count = count(new LambdaQueryWrapper<DocLibraryTag>()
                .eq(DocLibraryTag::getName, req.getName()));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT);
        }
        DocLibraryTag entity = new DocLibraryTag();
        entity.setName(req.getName());
        entity.setColor(req.getColor());
        save(entity);
        return toVO(entity);
    }

    @Override
    public DocLibraryTagVO updateTag(Long id, DocLibraryTagSaveRequest req) {
        DocLibraryTag entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        long count = count(new LambdaQueryWrapper<DocLibraryTag>()
                .eq(DocLibraryTag::getName, req.getName())
                .ne(DocLibraryTag::getId, id));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT);
        }
        entity.setName(req.getName());
        entity.setColor(req.getColor());
        updateById(entity);
        return toVO(entity);
    }

    @Override
    public void deleteTag(Long id) {
        DocLibraryTag entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        docLibraryFileTagMapper.deleteByFileId(id);
        removeById(id);
    }

    @Override
    @Transactional
    public void syncFileTags(Long fileId, List<Long> tagIds) {
        docLibraryFileTagMapper.deleteByFileId(fileId);
        if (tagIds != null && !tagIds.isEmpty()) {
            List<DocLibraryFileTag> rels = tagIds.stream()
                    .map(tagId -> {
                        DocLibraryFileTag rel = new DocLibraryFileTag();
                        rel.setFileId(fileId);
                        rel.setTagId(tagId);
                        return rel;
                    })
                    .toList();
            docLibraryFileTagMapper.insert(rels);
        }
    }

    @Override
    public List<DocLibraryTagVO> getTagsByFileId(Long fileId) {
        return docLibraryFileTagMapper.selectTagsByFileId(fileId)
                .stream()
                .map(this::toVO)
                .toList();
    }

    private DocLibraryTagVO toVO(DocLibraryTag entity) {
        DocLibraryTagVO vo = new DocLibraryTagVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setColor(entity.getColor());
        return vo;
    }
}
