package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.NbNoteTagSaveRequest;
import com.ai.manager.system.domain.entity.NbNoteTag;
import com.ai.manager.system.domain.entity.NbNoteTagRel;
import com.ai.manager.system.domain.vo.NbNoteTagVO;
import com.ai.manager.system.mapper.NbNoteTagMapper;
import com.ai.manager.system.mapper.NbNoteTagRelMapper;
import com.ai.manager.system.service.NbNoteTagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NbNoteTagServiceImpl extends ServiceImpl<NbNoteTagMapper, NbNoteTag> implements NbNoteTagService {

    private final NbNoteTagRelMapper nbNoteTagRelMapper;

    @Override
    public List<NbNoteTagVO> listAllTags() {
        return list(new LambdaQueryWrapper<NbNoteTag>()
                .orderByAsc(NbNoteTag::getName))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NbNoteTagVO createTag(NbNoteTagSaveRequest request) {
        validateTagRequest(request);
        NbNoteTag existing = getOne(new LambdaQueryWrapper<NbNoteTag>()
                .eq(NbNoteTag::getName, request.getName().trim())
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "标签已存在");
        }
        NbNoteTag tag = new NbNoteTag();
        tag.setName(request.getName().trim());
        tag.setColor(request.getColor());
        save(tag);
        return toVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NbNoteTagVO updateTag(Long id, NbNoteTagSaveRequest request) {
        NbNoteTag tag = getById(id);
        if (tag == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateTagRequest(request);
        tag.setName(request.getName().trim());
        tag.setColor(request.getColor());
        updateById(tag);
        return toVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        NbNoteTag tag = getById(id);
        if (tag == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        nbNoteTagRelMapper.delete(new LambdaQueryWrapper<NbNoteTagRel>().eq(NbNoteTagRel::getTagId, id));
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncNoteTags(Long noteId, List<Long> tagIds) {
        nbNoteTagRelMapper.deleteByNoteId(noteId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Set<Long> unique = new HashSet<>(tagIds);
        List<NbNoteTag> tags = listByIds(unique);
        if (tags.size() != unique.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "存在无效标签");
        }
        for (Long tagId : unique) {
            NbNoteTagRel rel = new NbNoteTagRel();
            rel.setNoteId(noteId);
            rel.setTagId(tagId);
            nbNoteTagRelMapper.insert(rel);
        }
    }

    @Override
    public List<NbNoteTagVO> listTagsByNoteId(Long noteId) {
        List<NbNoteTagRel> rels = nbNoteTagRelMapper.selectList(
                new LambdaQueryWrapper<NbNoteTagRel>().eq(NbNoteTagRel::getNoteId, noteId));
        if (rels.isEmpty()) {
            return List.of();
        }
        Set<Long> tagIds = rels.stream().map(NbNoteTagRel::getTagId).collect(Collectors.toSet());
        Map<Long, NbNoteTag> tagMap = listByIds(tagIds).stream()
                .collect(Collectors.toMap(NbNoteTag::getId, tag -> tag));
        List<NbNoteTagVO> result = new ArrayList<>();
        for (NbNoteTagRel rel : rels) {
            NbNoteTag tag = tagMap.get(rel.getTagId());
            if (tag != null) {
                result.add(toVO(tag));
            }
        }
        return result;
    }

    private NbNoteTagVO toVO(NbNoteTag tag) {
        NbNoteTagVO vo = new NbNoteTagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setColor(tag.getColor());
        vo.setCreateTime(tag.getCreateTime());
        return vo;
    }

    private void validateTagRequest(NbNoteTagSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "标签名称不能为空");
        }
    }
}
