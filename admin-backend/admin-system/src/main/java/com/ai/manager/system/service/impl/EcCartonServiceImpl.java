package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcCartonSaveRequest;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.EcCartonBackfillTaskVO;
import com.ai.manager.system.domain.vo.EcCartonCalculateResultVO;
import com.ai.manager.system.domain.vo.EcCartonListItemVO;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.service.EcCartonService;
import com.ai.manager.system.service.EcEcommerceImageRenameService;
import com.ai.manager.system.service.EcImageUploadService;
import com.ai.manager.system.service.support.EcCartonBackfillTaskManager;
import com.ai.manager.system.service.support.EcCartonMatcher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EcCartonServiceImpl extends ServiceImpl<EcCartonMapper, EcCarton> implements EcCartonService {

    private static final int BACKFILL_BATCH_SIZE = 50;

    private final EcFactoryMapper ecFactoryMapper;
    private final EcSkuMapper ecSkuMapper;
    private final EcProductMapper ecProductMapper;
    private final EcCartonBackfillTaskManager backfillTaskManager;
    private final EcEcommerceImageRenameService ecEcommerceImageRenameService;
    private final EcImageUploadService ecImageUploadService;

    @Override
    public PageResult<EcCartonListItemVO> pageCartons(String keyword, Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<EcCarton> wrapper = new LambdaQueryWrapper<EcCarton>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            Set<Long> matchedIds = collectCartonIdsByKeyword(kw);
            if (matchedIds.isEmpty()) {
                return PageResult.empty(p, ps);
            }
            wrapper.in(EcCarton::getId, matchedIds);
        }
        wrapper.last("ORDER BY (COALESCE(length_cm, 0) * COALESCE(width_cm, 0) * COALESCE(height_cm, 0)) DESC, id DESC");
        Page<EcCarton> entityPage = page(new Page<>(p, ps), wrapper);
        if (entityPage.getRecords().isEmpty()) {
            return PageUtils.of(List.of(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(entityPage.getRecords().stream()
                .map(EcCarton::getFactoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        List<EcCartonListItemVO> records = new ArrayList<>();
        for (EcCarton carton : entityPage.getRecords()) {
            records.add(toListItemVO(carton, factoryNameMap));
        }
        return PageUtils.of(records, entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());
    }

    @Override
    public EcCartonListItemVO getCartonDetail(Long id) {
        EcCarton carton = getById(id);
        if (carton == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(
                carton.getFactoryId() == null ? List.of() : List.of(carton.getFactoryId()));
        return toListItemVO(carton, factoryNameMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcCartonListItemVO createCarton(EcCartonSaveRequest request) {
        validateRequest(request);
        EcCarton carton = new EcCarton();
        applyFields(request, carton);
        save(carton);
        return getCartonDetail(carton.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcCartonListItemVO updateCarton(Long id, EcCartonSaveRequest request) {
        EcCarton existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateRequest(request);
        applyFields(request, existing);
        updateById(existing);
        return getCartonDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EcCartonListItemVO updateCartonPreviewImage(Long id, MultipartFile file, String cartonName) {
        EcCarton existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String nameForFile = StringUtils.hasText(cartonName) ? cartonName.trim() : existing.getName();
        if (!StringUtils.hasText(nameForFile)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "纸箱名称不能为空");
        }
        String storedName = ecImageUploadService.uploadCartonPreviewImage(file, nameForFile).getFileName();
        existing.setPreviewImage(storedName);
        updateById(existing);
        return getCartonDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCarton(Long id) {
        EcCarton existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long bound = ecSkuMapper.selectCount(new LambdaQueryWrapper<EcSku>()
                .eq(EcSku::getCartonId, id));
        if (bound != null && bound > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该纸箱仍被 SKU 使用，无法删除");
        }
        removeById(id);
    }

    @Override
    public EcCartonListItemVO matchCarton(java.math.BigDecimal lengthCm,
                                          java.math.BigDecimal widthCm,
                                          java.math.BigDecimal heightCm,
                                          Long factoryId) {
        List<EcCarton> all = list(new LambdaQueryWrapper<EcCarton>().orderByAsc(EcCarton::getId));
        List<EcCarton> preferred = EcCartonMatcher.preferFactoryCartons(all, factoryId);
        EcCarton matched = EcCartonMatcher.findBestFit(preferred, lengthCm, widthCm, heightCm);
        if (matched == null && factoryId != null) {
            matched = EcCartonMatcher.findBestFit(all, lengthCm, widthCm, heightCm);
        }
        if (matched == null) {
            return null;
        }
        Map<Long, String> factoryNameMap = loadFactoryNameMap(
                matched.getFactoryId() == null ? List.of() : List.of(matched.getFactoryId()));
        return toListItemVO(matched, factoryNameMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int backfillSkuCartons() {
        List<EcSku> skus = loadAllSkus();
        if (skus.isEmpty()) {
            return 0;
        }
        Map<Long, Long> productFactoryMap = loadProductFactoryMap();
        return backfillSkus(skus, productFactoryMap);
    }

    @Override
    public EcCartonCalculateResultVO calculateCartons(java.math.BigDecimal lengthCm,
                                                      java.math.BigDecimal widthCm,
                                                      java.math.BigDecimal heightCm,
                                                      Long factoryId) {
        EcCartonListItemVO matched = matchCarton(lengthCm, widthCm, heightCm, factoryId);
        EcCartonCalculateResultVO result = new EcCartonCalculateResultVO();
        result.setMatchedCarton(matched);
        // 库存纸箱暂与匹配结果一致，后续接入库存逻辑后在此独立计算
        result.setInventoryCarton(matched);
        return result;
    }

    @Override
    public String startBackfillSkuCartonsAsync() {
        return backfillTaskManager.startTask(this::runBackfillInBatches);
    }

    @Override
    public EcCartonBackfillTaskVO getBackfillTask(String taskId) {
        return backfillTaskManager.getTask(taskId);
    }

    private void runBackfillInBatches(EcCartonBackfillTaskVO task) {
        List<EcSku> skus = loadAllSkus();
        Map<Long, Long> productFactoryMap = loadProductFactoryMap();
        task.setTotal(skus.size());
        task.setProcessed(0);
        task.setUpdated(0);

        if (skus.isEmpty()) {
            return;
        }

        int updatedTotal = 0;
        for (int i = 0; i < skus.size(); i += BACKFILL_BATCH_SIZE) {
            int end = Math.min(i + BACKFILL_BATCH_SIZE, skus.size());
            List<EcSku> batch = skus.subList(i, end);
            updatedTotal += backfillSkus(batch, productFactoryMap);
            task.setProcessed(end);
            task.setUpdated(updatedTotal);
        }
    }

    private List<EcSku> loadAllSkus() {
        return ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>().orderByAsc(EcSku::getId));
    }

    private Map<Long, Long> loadProductFactoryMap() {
        return ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>())
                .stream()
                .collect(Collectors.toMap(EcProduct::getId, EcProduct::getFactoryId, (a, b) -> a));
    }

    private int backfillSkus(List<EcSku> skus, Map<Long, Long> productFactoryMap) {
        int updated = 0;
        for (EcSku sku : skus) {
            Long factoryId = productFactoryMap.get(sku.getProductId());
            EcCartonListItemVO matched = matchCarton(
                    sku.getProductLengthCm(),
                    sku.getProductWidthCm(),
                    sku.getProductHeightCm(),
                    factoryId);
            Long matchedId = matched != null ? matched.getId() : null;
            if (!Objects.equals(matchedId, sku.getCartonId())) {
                sku.setCartonId(matchedId);
                ecSkuMapper.updateById(sku);
                updated++;
            }
        }
        return updated;
    }

    private Set<Long> collectCartonIdsByKeyword(String keyword) {
        Set<Long> matchedIds = new HashSet<>();

        list(new LambdaQueryWrapper<EcCarton>().like(EcCarton::getName, keyword))
                .forEach(carton -> matchedIds.add(carton.getId()));

        list(new LambdaQueryWrapper<EcCarton>().like(EcCarton::getRemark, keyword))
                .forEach(carton -> matchedIds.add(carton.getId()));

        List<EcFactory> factories = ecFactoryMapper.selectList(
                new LambdaQueryWrapper<EcFactory>().like(EcFactory::getName, keyword));
        if (!factories.isEmpty()) {
            List<Long> factoryIds = factories.stream().map(EcFactory::getId).toList();
            list(new LambdaQueryWrapper<EcCarton>().in(EcCarton::getFactoryId, factoryIds))
                    .forEach(carton -> matchedIds.add(carton.getId()));
        }

        return matchedIds;
    }

    private EcCartonListItemVO toListItemVO(EcCarton carton, Map<Long, String> factoryNameMap) {
        EcCartonListItemVO vo = new EcCartonListItemVO();
        vo.setId(carton.getId());
        vo.setFactoryId(carton.getFactoryId());
        if (carton.getFactoryId() != null) {
            vo.setFactoryName(factoryNameMap.get(carton.getFactoryId()));
        }
        vo.setName(carton.getName());
        vo.setLengthCm(carton.getLengthCm());
        vo.setWidthCm(carton.getWidthCm());
        vo.setHeightCm(carton.getHeightCm());
        vo.setUnitPrice(carton.getUnitPrice());
        vo.setRemark(carton.getRemark());
        vo.setIllustrationVariant(carton.getIllustrationVariant());
        vo.setPreviewImage(carton.getPreviewImage());
        vo.setUpdateTime(carton.getUpdateTime());
        return vo;
    }

    private void applyFields(EcCartonSaveRequest request, EcCarton carton) {
        carton.setName(request.getName().trim());
        carton.setFactoryId(resolveFactoryId(request.getFactoryId()));
        carton.setLengthCm(request.getLengthCm());
        carton.setWidthCm(request.getWidthCm());
        carton.setHeightCm(request.getHeightCm());
        carton.setUnitPrice(request.getUnitPrice());
        carton.setRemark(trimToNull(request.getRemark()));
        carton.setIllustrationVariant(normalizeIllustrationVariant(request.getIllustrationVariant()));
        if (StringUtils.hasText(request.getPreviewImage())) {
            carton.setPreviewImage(ecEcommerceImageRenameService.normalizeCartonPreview(
                    request.getPreviewImage().trim(), request.getName().trim()));
        }
    }

    private void validateRequest(EcCartonSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "纸箱名称不能为空");
        }
        if (request.getFactoryId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择所属工厂");
        }
        resolveFactoryId(request.getFactoryId());
        if (request.getUnitPrice() != null && request.getUnitPrice().signum() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "单价不能为负数");
        }
        if (request.getIllustrationVariant() != null
                && (request.getIllustrationVariant() < 0 || request.getIllustrationVariant() > 3)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "纸箱材质无效");
        }
    }

    private Integer normalizeIllustrationVariant(Integer variant) {
        if (variant == null) {
            return 3;
        }
        if (variant < 0 || variant > 3) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "纸箱材质无效");
        }
        return variant;
    }

    private Long resolveFactoryId(Long factoryId) {
        if (factoryId == null) {
            return null;
        }
        EcFactory factory = ecFactoryMapper.selectById(factoryId);
        if (factory == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工厂不存在");
        }
        String factoryType = factory.getFactoryType();
        if (factoryType == null || !"CARTON".equals(factoryType.trim().toUpperCase())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择纸箱厂类型的工厂");
        }
        return factoryId;
    }

    private Map<Long, String> loadFactoryNameMap(List<Long> factoryIds) {
        if (factoryIds == null || factoryIds.isEmpty()) {
            return Map.of();
        }
        return ecFactoryMapper.selectBatchIds(factoryIds).stream()
                .collect(Collectors.toMap(EcFactory::getId, EcFactory::getName, (a, b) -> a));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
