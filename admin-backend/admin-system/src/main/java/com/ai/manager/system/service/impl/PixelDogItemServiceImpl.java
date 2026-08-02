package com.ai.manager.system.service.impl;

import com.ai.manager.system.domain.entity.PixelDogItem;
import com.ai.manager.system.domain.vo.PixelDogItemVO;
import com.ai.manager.system.mapper.PixelDogItemMapper;
import com.ai.manager.system.service.PixelDogItemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PixelDogItemServiceImpl implements PixelDogItemService {

    private final PixelDogItemMapper pixelDogItemMapper;

    @Override
    public List<PixelDogItemVO> listItems() {
        List<PixelDogItem> list = pixelDogItemMapper.selectList(
                new LambdaQueryWrapper<PixelDogItem>()
                        .eq(PixelDogItem::getDeleted, 0)
                        .orderByAsc(PixelDogItem::getSortOrder)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PixelDogItemVO createItem(PixelDogItem item) {
        item.setId(null);
        item.setDeleted(0);
        if (item.getSortOrder() == null) item.setSortOrder(0);
        if (item.getShape() == null) item.setShape(0);
        pixelDogItemMapper.insert(item);
        return toVO(item);
    }

    @Override
    @Transactional
    public PixelDogItemVO updateItem(PixelDogItem item) {
        PixelDogItem existing = pixelDogItemMapper.selectById(item.getId());
        if (existing == null || existing.getDeleted() == 1) {
            throw new RuntimeException("物品不存在");
        }
        if (item.getName() != null) existing.setName(item.getName());
        if (item.getIcon() != null) existing.setIcon(item.getIcon());
        if (item.getColor() != null) existing.setColor(item.getColor());
        if (item.getRequireLevel() != null) existing.setRequireLevel(item.getRequireLevel());
        if (item.getSortOrder() != null) existing.setSortOrder(item.getSortOrder());
        if (item.getShape() != null) existing.setShape(item.getShape());
        pixelDogItemMapper.updateById(existing);
        return toVO(existing);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        pixelDogItemMapper.deleteById(id);
    }

    private PixelDogItemVO toVO(PixelDogItem entity) {
        PixelDogItemVO vo = new PixelDogItemVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setIcon(entity.getIcon());
        vo.setColor(entity.getColor());
        vo.setRequireLevel(entity.getRequireLevel());
        vo.setSortOrder(entity.getSortOrder());
        vo.setShape(entity.getShape());
        return vo;
    }
}
