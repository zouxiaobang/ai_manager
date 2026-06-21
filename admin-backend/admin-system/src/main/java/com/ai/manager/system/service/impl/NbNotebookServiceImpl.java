package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.NbNotebookSaveRequest;
import com.ai.manager.system.domain.entity.NbNotebook;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.vo.NbTreeNodeVO;
import com.ai.manager.system.mapper.NbNoteMapper;
import com.ai.manager.system.mapper.NbNotebookMapper;
import com.ai.manager.system.service.NbNotebookService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NbNotebookServiceImpl extends ServiceImpl<NbNotebookMapper, NbNotebook>
        implements NbNotebookService {

    private static final long ROOT_KEY = 0L;

    private final NbNoteMapper nbNoteMapper;

    @Override
    public List<NbTreeNodeVO> buildTree() {
        List<NbNotebook> notebooks = list(new LambdaQueryWrapper<NbNotebook>()
                .orderByAsc(NbNotebook::getSortOrder)
                .orderByAsc(NbNotebook::getId));
        List<NbNote> notes = nbNoteMapper.selectList(new LambdaQueryWrapper<NbNote>()
                .orderByDesc(NbNote::getIsPinned)
                .orderByAsc(NbNote::getSortOrder)
                .orderByAsc(NbNote::getId));

        Map<Long, List<NbNotebook>> foldersByParent = notebooks.stream()
                .collect(Collectors.groupingBy(nb -> nb.getParentId() == null ? ROOT_KEY : nb.getParentId()));
        Map<Long, List<NbNote>> notesByFolder = notes.stream()
                .collect(Collectors.groupingBy(note -> note.getNotebookId() == null ? ROOT_KEY : note.getNotebookId()));

        return buildFolderNodes(ROOT_KEY, foldersByParent, notesByFolder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NbNotebook createNotebook(NbNotebookSaveRequest request) {
        validateNotebookRequest(request);
        if (request.getParentId() != null) {
            NbNotebook parent = getById(request.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "父文件夹不存在");
            }
        }
        NbNotebook notebook = new NbNotebook();
        notebook.setParentId(request.getParentId());
        notebook.setName(request.getName().trim());
        notebook.setIcon(request.getIcon());
        notebook.setColor(request.getColor());
        notebook.setSortOrder(request.getSortOrder() == null ? nextSortOrder(request.getParentId()) : request.getSortOrder());
        save(notebook);
        return notebook;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NbNotebook updateNotebook(Long id, NbNotebookSaveRequest request) {
        NbNotebook existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateNotebookRequest(request);
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能将文件夹移动到自身");
            }
            NbNotebook parent = getById(request.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "父文件夹不存在");
            }
            if (isDescendant(id, request.getParentId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能将文件夹移动到其子文件夹下");
            }
        }
        existing.setParentId(request.getParentId());
        existing.setName(request.getName().trim());
        existing.setIcon(request.getIcon());
        existing.setColor(request.getColor());
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }
        updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotebook(Long id) {
        NbNotebook notebook = getById(id);
        if (notebook == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Set<Long> folderIds = collectDescendantFolderIds(id);
        if (!folderIds.isEmpty()) {
            nbNoteMapper.delete(new LambdaQueryWrapper<NbNote>().in(NbNote::getNotebookId, folderIds));
        }
        List<Long> deleteOrder = orderFoldersLeafFirst(folderIds);
        for (Long folderId : deleteOrder) {
            removeById(folderId);
        }
    }

    private List<NbTreeNodeVO> buildFolderNodes(Long parentKey,
                                                Map<Long, List<NbNotebook>> foldersByParent,
                                                Map<Long, List<NbNote>> notesByFolder) {
        List<NbTreeNodeVO> result = new ArrayList<>();
        List<NbNotebook> folders = foldersByParent.getOrDefault(parentKey, List.of());
        for (NbNotebook folder : folders) {
            NbTreeNodeVO node = toFolderNode(folder);
            List<NbTreeNodeVO> children = new ArrayList<>();
            children.addAll(buildFolderNodes(folder.getId(), foldersByParent, notesByFolder));
            children.addAll(toNoteNodes(notesByFolder.getOrDefault(folder.getId(), List.of())));
            node.setChildren(children);
            result.add(node);
        }
        if (parentKey == ROOT_KEY) {
            result.addAll(toNoteNodes(notesByFolder.getOrDefault(ROOT_KEY, List.of())));
        }
        return result;
    }

    private NbTreeNodeVO toFolderNode(NbNotebook folder) {
        NbTreeNodeVO node = new NbTreeNodeVO();
        node.setNodeKey("folder-" + folder.getId());
        node.setNodeType("FOLDER");
        node.setNotebookId(folder.getId());
        node.setParentId(folder.getParentId());
        node.setName(folder.getName());
        return node;
    }

    private List<NbTreeNodeVO> toNoteNodes(List<NbNote> notes) {
        List<NbTreeNodeVO> nodes = new ArrayList<>();
        for (NbNote note : notes) {
            NbTreeNodeVO node = new NbTreeNodeVO();
            node.setNodeKey("note-" + note.getId());
            node.setNodeType("NOTE");
            node.setNoteId(note.getId());
            node.setNotebookId(note.getNotebookId());
            node.setName(StringUtils.hasText(note.getTitle()) ? note.getTitle() : "无标题");
            node.setIsPinned(note.getIsPinned());
            node.setIsFavorite(note.getIsFavorite());
            nodes.add(node);
        }
        return nodes;
    }

    private void validateNotebookRequest(NbNotebookSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件夹名称不能为空");
        }
    }

    private int nextSortOrder(Long parentId) {
        NbNotebook last = getOne(new LambdaQueryWrapper<NbNotebook>()
                .eq(parentId != null, NbNotebook::getParentId, parentId)
                .isNull(parentId == null, NbNotebook::getParentId)
                .orderByDesc(NbNotebook::getSortOrder)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 0 : last.getSortOrder() + 1;
    }

    private Set<Long> collectDescendantFolderIds(Long rootId) {
        List<NbNotebook> all = list();
        Map<Long, List<NbNotebook>> childrenMap = all.stream()
                .filter(nb -> nb.getParentId() != null)
                .collect(Collectors.groupingBy(NbNotebook::getParentId));
        Set<Long> result = new HashSet<>();
        collectFolderIds(rootId, childrenMap, result);
        return result;
    }

    private void collectFolderIds(Long folderId, Map<Long, List<NbNotebook>> childrenMap, Set<Long> result) {
        result.add(folderId);
        for (NbNotebook child : childrenMap.getOrDefault(folderId, List.of())) {
            collectFolderIds(child.getId(), childrenMap, result);
        }
    }

    private List<Long> orderFoldersLeafFirst(Set<Long> folderIds) {
        List<NbNotebook> folders = listByIds(folderIds);
        Map<Long, Long> parentMap = folders.stream()
                .collect(Collectors.toMap(NbNotebook::getId, nb -> nb.getParentId() == null ? ROOT_KEY : nb.getParentId()));
        return folderIds.stream()
                .sorted(Comparator.comparingInt((Long id) -> depth(id, parentMap, folderIds)).reversed())
                .toList();
    }

    private int depth(Long id, Map<Long, Long> parentMap, Set<Long> folderIds) {
        int depth = 0;
        Long current = id;
        while (folderIds.contains(current) && parentMap.containsKey(current) && parentMap.get(current) != ROOT_KEY) {
            depth++;
            current = parentMap.get(current);
            if (!folderIds.contains(current)) {
                break;
            }
        }
        return depth;
    }

    private boolean isDescendant(Long ancestorId, Long targetParentId) {
        Long current = targetParentId;
        while (current != null) {
            if (current.equals(ancestorId)) {
                return true;
            }
            NbNotebook folder = getById(current);
            if (folder == null) {
                break;
            }
            current = folder.getParentId();
        }
        return false;
    }
}
