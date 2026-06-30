package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcFactorySaveRequest;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.vo.EcFactoryStatsVO;
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
    public PageResult<EcFactory> pageFactories(String keyword, String factoryType, String status, Long page, Long pageSize) {
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
        applyFactoryTypeFilter(wrapper, factoryType);
        if (StringUtils.hasText(status)) {
            wrapper.eq(EcFactory::getStatus, status.trim().toUpperCase());
        }
        Page<EcFactory> result = page(new Page<>(p, ps), wrapper);
        return PageUtils.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public EcFactoryStatsVO getFactoryStats() {
        EcFactoryStatsVO stats = new EcFactoryStatsVO();
        stats.setProductionCount(count(new LambdaQueryWrapper<EcFactory>()
                .and(w -> w.eq(EcFactory::getFactoryType, "PRODUCTION").or().isNull(EcFactory::getFactoryType))));
        stats.setCustomerCount(count(new LambdaQueryWrapper<EcFactory>()
                .eq(EcFactory::getFactoryType, "CUSTOMER")));
        stats.setCartonCount(count(new LambdaQueryWrapper<EcFactory>()
                .eq(EcFactory::getFactoryType, "CARTON")));
        stats.setEnabledCount(count(new LambdaQueryWrapper<EcFactory>()
                .and(w -> w.eq(EcFactory::getStatus, "ENABLED").or().isNull(EcFactory::getStatus))));
        stats.setDisabledCount(count(new LambdaQueryWrapper<EcFactory>()
                .eq(EcFactory::getStatus, "DISABLED")));
        return stats;
    }

    @Override
    public List<EcFactory> listFactoryOptions(String factoryType) {
        LambdaQueryWrapper<EcFactory> wrapper = new LambdaQueryWrapper<EcFactory>()
                .select(EcFactory::getId, EcFactory::getName, EcFactory::getFactoryType, EcFactory::getStatus)
                .orderByDesc(EcFactory::getId);
        applyFactoryTypeFilter(wrapper, factoryType);
        return list(wrapper);
    }

    private void applyFactoryTypeFilter(LambdaQueryWrapper<EcFactory> wrapper, String factoryType) {
        if (!StringUtils.hasText(factoryType)) {
            return;
        }
        String type = factoryType.trim().toUpperCase();
        if ("PRODUCTION".equals(type)) {
            wrapper.and(w -> w.eq(EcFactory::getFactoryType, type).or().isNull(EcFactory::getFactoryType));
        } else if ("CUSTOMER".equals(type)) {
            wrapper.eq(EcFactory::getFactoryType, type);
        } else if ("CARTON".equals(type)) {
            wrapper.eq(EcFactory::getFactoryType, type);
        }
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
        String factoryType = normalizeFactoryType(request.getFactoryType());
        if (!"PRODUCTION".equals(factoryType) && !"CUSTOMER".equals(factoryType) && !"CARTON".equals(factoryType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工厂类型无效");
        }
    }

    private String normalizeFactoryType(String factoryType) {
        if (!StringUtils.hasText(factoryType)) {
            return "PRODUCTION";
        }
        return factoryType.trim().toUpperCase();
    }

    private EcFactory toEntity(EcFactorySaveRequest request, EcFactory factory) {
        factory.setName(request.getName().trim());
        factory.setFactoryType(normalizeFactoryType(request.getFactoryType()));
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
