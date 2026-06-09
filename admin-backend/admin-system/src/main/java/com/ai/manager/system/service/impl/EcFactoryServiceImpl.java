package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcFactorySaveRequest;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.service.EcFactoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EcFactoryServiceImpl extends ServiceImpl<EcFactoryMapper, EcFactory> implements EcFactoryService {

    private final EcProductMapper ecProductMapper;
    private final EcCartonMapper ecCartonMapper;

    @Override
    public PageResult<EcFactory> pageFactories(String keyword, Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcFactory> wrapper = new LambdaQueryWrapper<EcFactory>()
                .orderByDesc(EcFactory::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(EcFactory::getName, kw)
                    .or().like(EcFactory::getContactName, kw)
                    .or().like(EcFactory::getContactPhone, kw));
        }
        Page<EcFactory> result = page(new Page<>(p, ps), wrapper);
        return PageUtils.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<EcFactory> listFactoryOptions() {
        return list(new LambdaQueryWrapper<EcFactory>()
                .select(EcFactory::getId, EcFactory::getName, EcFactory::getStatus)
                .orderByDesc(EcFactory::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcFactory createFactory(EcFactorySaveRequest request) {
        validateRequest(request);
        EcFactory factory = toEntity(request, new EcFactory());
        save(factory);
        return factory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcFactory updateFactory(Long id, EcFactorySaveRequest request) {
        EcFactory existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateRequest(request);
        EcFactory factory = toEntity(request, existing);
        updateById(factory);
        return factory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFactory(Long id) {
        EcFactory existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long bound = ecProductMapper.selectCount(new LambdaQueryWrapper<EcProduct>()
                .eq(EcProduct::getFactoryId, id));
        if (bound != null && bound > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该工厂下仍有商品，无法删除");
        }
        Long cartonBound = ecCartonMapper.selectCount(new LambdaQueryWrapper<EcCarton>()
                .eq(EcCarton::getFactoryId, id));
        if (cartonBound != null && cartonBound > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该工厂下仍有纸箱，无法删除");
        }
        removeById(id);
    }

    private void validateRequest(EcFactorySaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工厂名称不能为空");
        }
    }

    private EcFactory toEntity(EcFactorySaveRequest request, EcFactory factory) {
        factory.setName(request.getName().trim());
        factory.setContactName(trimToNull(request.getContactName()));
        factory.setContactPhone(trimToNull(request.getContactPhone()));
        factory.setAddress(trimToNull(request.getAddress()));
        factory.setRemark(trimToNull(request.getRemark()));
        if (StringUtils.hasText(request.getStatus())) {
            factory.setStatus(request.getStatus().trim().toUpperCase());
        } else if (factory.getStatus() == null) {
            factory.setStatus("ENABLED");
        }
        return factory;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
