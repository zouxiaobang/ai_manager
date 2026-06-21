package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.NbTodoSaveRequest;
import com.ai.manager.system.domain.entity.NbTodoItem;
import com.ai.manager.system.domain.enums.NbTodoRepeatType;
import com.ai.manager.system.domain.vo.NbTodoItemVO;
import com.ai.manager.system.domain.vo.NbTodoMutationVO;
import com.ai.manager.system.mapper.NbTodoItemMapper;
import com.ai.manager.system.service.NbTodoService;
import com.ai.manager.system.service.support.TodoRepeatSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NbTodoServiceImpl extends ServiceImpl<NbTodoItemMapper, NbTodoItem> implements NbTodoService {

    @Override
    public List<NbTodoItemVO> list(Boolean completed, Boolean today) {
        LambdaQueryWrapper<NbTodoItem> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(today)) {
            wrapper.eq(NbTodoItem::getCompleted, 0);
            applyTodayFilter(wrapper);
        } else if (completed != null) {
            wrapper.eq(NbTodoItem::getCompleted, completed ? 1 : 0);
        }
        wrapper.last("""
                ORDER BY completed ASC,
                (due_time IS NULL AND remind_time IS NULL) ASC,
                COALESCE(due_time, remind_time) ASC,
                create_time DESC
                """);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<NbTodoItemVO> listToday() {
        return list(null, true);
    }

    @Override
    public List<NbTodoItemVO> listDueReminders() {
        LambdaQueryWrapper<NbTodoItem> wrapper = new LambdaQueryWrapper<NbTodoItem>()
                .eq(NbTodoItem::getCompleted, 0)
                .isNotNull(NbTodoItem::getRemindTime)
                .le(NbTodoItem::getRemindTime, LocalDateTime.now())
                .and(w -> w.isNull(NbTodoItem::getRemindNotified).or().eq(NbTodoItem::getRemindNotified, 0))
                .orderByAsc(NbTodoItem::getRemindTime);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ackRemind(Long id) {
        NbTodoItem item = getById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        item.setRemindNotified(1);
        updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NbTodoItemVO create(NbTodoSaveRequest request) {
        String content = request == null || request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "待办内容不能为空");
        }

        NbTodoItem item = new NbTodoItem();
        item.setContent(content);
        item.setCompleted(request != null && Boolean.TRUE.equals(request.getCompleted()) ? 1 : 0);
        item.setRepeatType(NbTodoRepeatType.NONE.name());
        item.setRepeatInterval(1);
        item.setRemindNotified(0);
        applyScheduleFields(item, request);
        item.setSortOrder(resolveNextSortOrder(request == null ? null : request.getSortOrder()));
        save(item);

        if (TodoRepeatSupport.isRecurring(item) && item.getSeriesId() == null) {
            item.setSeriesId(item.getId());
            updateById(item);
        }
        return toVO(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NbTodoMutationVO update(Long id, NbTodoSaveRequest request) {
        NbTodoItem item = getById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        LocalDateTime previousRemindTime = item.getRemindTime();
        boolean wasPending = item.getCompleted() == null || item.getCompleted() == 0;
        if (request != null) {
            if (request.getContent() != null) {
                String content = request.getContent().trim();
                if (!StringUtils.hasText(content)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "待办内容不能为空");
                }
                item.setContent(content);
            }
            if (request.getSortOrder() != null) {
                item.setSortOrder(request.getSortOrder());
            }
            applyScheduleFields(item, request);

            if (request.getCompleted() != null) {
                item.setCompleted(request.getCompleted() ? 1 : 0);
            }
        }

        if (item.getRemindTime() != null && !item.getRemindTime().equals(previousRemindTime)) {
            item.setRemindNotified(0);
        }
        if (Boolean.TRUE.equals(request != null ? request.getClearRemindTime() : null)) {
            item.setRemindNotified(0);
        }

        updateById(item);

        NbTodoMutationVO mutation = new NbTodoMutationVO();
        mutation.setItem(toVO(item));

        if (request != null
                && Boolean.TRUE.equals(request.getCompleted())
                && wasPending
                && TodoRepeatSupport.isRecurring(item)) {
            NbTodoItem next = TodoRepeatSupport.buildNextOccurrence(item);
            if (next != null) {
                next.setRemindNotified(0);
                save(next);
                mutation.setNextOccurrence(toVO(next));
            }
        }
        return mutation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        NbTodoItem item = getById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        removeById(id);
    }

    private void applyTodayFilter(LambdaQueryWrapper<NbTodoItem> wrapper) {
        wrapper.and(w -> w
                .apply("(due_time >= CURDATE() AND due_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY))")
                .or()
                .apply("(remind_time >= CURDATE() AND remind_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY))"));
    }

    private void applyScheduleFields(NbTodoItem item, NbTodoSaveRequest request) {
        if (request == null) {
            return;
        }
        if (Boolean.TRUE.equals(request.getClearDueTime())) {
            item.setDueTime(null);
        } else if (request.getDueTime() != null) {
            item.setDueTime(request.getDueTime());
        }

        if (Boolean.TRUE.equals(request.getClearRemindTime())) {
            item.setRemindTime(null);
            item.setRemindNotified(0);
        } else if (request.getRemindTime() != null) {
            item.setRemindTime(request.getRemindTime());
            item.setRemindNotified(0);
        }

        if (request.getRepeatType() != null) {
            item.setRepeatType(NbTodoRepeatType.fromValue(request.getRepeatType()).name());
        }
        if (request.getRepeatInterval() != null) {
            item.setRepeatInterval(request.getRepeatInterval() < 1 ? 1 : request.getRepeatInterval());
        }

        if (Boolean.TRUE.equals(request.getClearRepeatUntil())) {
            item.setRepeatUntil(null);
        } else if (request.getRepeatUntil() != null) {
            item.setRepeatUntil(request.getRepeatUntil());
        }

        if (!TodoRepeatSupport.isRecurring(item)) {
            item.setRepeatInterval(1);
            item.setRepeatUntil(null);
        }
    }

    private int resolveNextSortOrder(Integer requested) {
        if (requested != null) {
            return requested;
        }
        NbTodoItem last = getOne(new LambdaQueryWrapper<NbTodoItem>()
                .orderByDesc(NbTodoItem::getSortOrder)
                .last("LIMIT 1"));
        return last == null ? 0 : last.getSortOrder() + 1;
    }

    private NbTodoItemVO toVO(NbTodoItem item) {
        NbTodoItemVO vo = new NbTodoItemVO();
        vo.setId(item.getId());
        vo.setContent(item.getContent());
        vo.setCompleted(item.getCompleted());
        vo.setDueTime(item.getDueTime());
        vo.setRemindTime(item.getRemindTime());
        vo.setRepeatType(item.getRepeatType() == null ? NbTodoRepeatType.NONE.name() : item.getRepeatType());
        vo.setRepeatInterval(item.getRepeatInterval() == null ? 1 : item.getRepeatInterval());
        vo.setRepeatUntil(item.getRepeatUntil());
        vo.setSeriesId(item.getSeriesId());
        vo.setSortOrder(item.getSortOrder());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }
}
