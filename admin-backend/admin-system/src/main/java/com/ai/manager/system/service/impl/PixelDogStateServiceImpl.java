package com.ai.manager.system.service.impl;

import com.ai.manager.system.domain.entity.PixelDogItem;
import com.ai.manager.system.domain.entity.PixelDogState;
import com.ai.manager.system.domain.vo.PixelDogStateVO;
import com.ai.manager.system.mapper.PixelDogItemMapper;
import com.ai.manager.system.mapper.PixelDogStateMapper;
import com.ai.manager.system.service.PixelDogStateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PixelDogStateServiceImpl implements PixelDogStateService {

    private final PixelDogStateMapper pixelDogStateMapper;

    private final PixelDogItemMapper pixelDogItemMapper;

    @Override
    public PixelDogStateVO getState() {
        PixelDogState entity = getActiveEntity();
        if (entity == null) {
            return null;
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public PixelDogStateVO updateState(PixelDogState state) {
        PixelDogState existing = getActiveEntity();
        if (existing == null) {
            existing = new PixelDogState();
            existing.setDeleted(0);
        }

        if (state.getLevel() != null) {
            existing.setLevel(state.getLevel());
        }
        if (state.getXp() != null) {
            existing.setXp(state.getXp());
        }
        if (state.getXpNext() != null) {
            existing.setXpNext(state.getXpNext());
        }
        if (state.getBond() != null) {
            existing.setBond(state.getBond());
        }
        if (state.getEmotion() != null) {
            existing.setEmotion(state.getEmotion());
        }
        if (state.getLastInteractTs() != null) {
            existing.setLastInteractTs(state.getLastInteractTs());
        }
        if (state.getLastGreetTs() != null) {
            existing.setLastGreetTs(state.getLastGreetTs());
        }
        if (state.getStatus() != null) {
            existing.setStatus(state.getStatus());
        }
        if (state.getUnlockedItems() != null) {
            existing.setUnlockedItems(state.getUnlockedItems());
        }
        if (state.getEquippedItems() != null) {
            existing.setEquippedItems(state.getEquippedItems());
        }

        if (existing.getId() == null) {
            pixelDogStateMapper.insert(existing);
        } else {
            pixelDogStateMapper.updateById(existing);
        }

        return toVO(existing);
    }

    @Override
    public PixelDogState getActiveEntity() {
        List<PixelDogState> list = pixelDogStateMapper.selectList(
                new LambdaQueryWrapper<PixelDogState>()
                        .eq(PixelDogState::getDeleted, 0)
                        .last("LIMIT 1")
        );
        return list.isEmpty() ? null : list.get(0);
    }

    private PixelDogStateVO toVO(PixelDogState entity) {
        PixelDogStateVO vo = new PixelDogStateVO();
        vo.setLevel(entity.getLevel());
        vo.setXp(entity.getXp());
        vo.setXpNext(entity.getXpNext());
        vo.setBond(entity.getBond());
        vo.setEmotion(entity.getEmotion());
        vo.setLastInteractTs(entity.getLastInteractTs());
        vo.setLastGreetTs(entity.getLastGreetTs());

        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(0, 0)) && now.isBefore(LocalTime.of(7, 0))) {
            vo.setStatus(3);
        } else {
            vo.setStatus(entity.getStatus());
        }

        vo.setUnlockedItems(entity.getUnlockedItems());
        vo.setEquippedItems(entity.getEquippedItems() != null ? entity.getEquippedItems() : 0L);
        return vo;
    }

    @Override
    @Transactional
    public PixelDogStateVO addXp(String action, int xpAmount) {
        PixelDogState existing = getActiveEntity();
        if (existing == null) {
            existing = new PixelDogState();
            existing.setDeleted(0);
            existing.setLevel(1);
            existing.setXp(0);
            existing.setXpNext(100);
            existing.setBond(0);
            existing.setEmotion(0);
            existing.setStatus(0);
            existing.setUnlockedItems(0);
        }

        existing.setXp(existing.getXp() + xpAmount);

        while (existing.getXp() >= existing.getXpNext()) {
            existing.setXp(existing.getXp() - existing.getXpNext());
            existing.setLevel(existing.getLevel() + 1);
            existing.setXpNext(existing.getLevel() * 100 + (existing.getLevel() - 1) * 50);
            existing.setUnlockedItems(Math.min(5, existing.getLevel()));
            // 自动装备新解锁的物品
            List<PixelDogItem> items = pixelDogItemMapper.selectList(
                    new LambdaQueryWrapper<PixelDogItem>()
                            .eq(PixelDogItem::getDeleted, 0)
                            .orderByAsc(PixelDogItem::getSortOrder)
            );
            long equipped = existing.getEquippedItems() != null ? existing.getEquippedItems() : 0L;
            for (PixelDogItem item : items) {
                int reqLevel = item.getRequireLevel();
                if (existing.getLevel() >= reqLevel && (equipped & (1L << (item.getId() - 1))) == 0) {
                    equipped |= (1L << (item.getId() - 1));
                }
            }
            existing.setEquippedItems(equipped);
        }

        if (existing.getId() == null) {
            pixelDogStateMapper.insert(existing);
        } else {
            pixelDogStateMapper.updateById(existing);
        }

        return toVO(existing);
    }

    @Override
    @Transactional
    public PixelDogStateVO interact(String action) {
        PixelDogState existing = getActiveEntity();
        if (existing == null) {
            existing = new PixelDogState();
            existing.setDeleted(0);
            existing.setLevel(1);
            existing.setXp(0);
            existing.setXpNext(100);
            existing.setBond(0);
            existing.setEmotion(0);
            existing.setStatus(0);
            existing.setUnlockedItems(0);
        }

        long now = System.currentTimeMillis() / 1000;

        switch (action) {
            case "pet":
                existing.setEmotion(Math.min(100, existing.getEmotion() + 10));
                existing.setBond(Math.min(100, existing.getBond() + 1));
                existing.setLastInteractTs(now);
                break;
            case "greet":
                existing.setBond(Math.min(100, existing.getBond() + 5));
                existing.setEmotion(Math.min(100, existing.getEmotion() + 5));
                existing.setLastGreetTs(now);
                existing.setLastInteractTs(now);
                break;
            case "nuzzle":
                existing.setEmotion(Math.min(100, existing.getEmotion() + 15));
                existing.setBond(Math.min(100, existing.getBond() + 3));
                existing.setLastInteractTs(now);
                break;
            case "hug":
                existing.setEmotion(Math.min(100, existing.getEmotion() + 30));
                existing.setBond(Math.min(100, existing.getBond() + 5));
                existing.setLastInteractTs(now);
                break;
            default:
                return toVO(existing);
        }

        if (existing.getId() == null) {
            pixelDogStateMapper.insert(existing);
        } else {
            pixelDogStateMapper.updateById(existing);
        }

        return toVO(existing);
    }
}
