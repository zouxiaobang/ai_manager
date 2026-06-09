package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.EcInventory;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.EcListingLinkSkuInventoryVO;
import com.ai.manager.system.mapper.EcInventoryMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EcListingLinkInventorySupport {

    private final EcSkuMapper ecSkuMapper;
    private final EcProductMapper ecProductMapper;
    private final EcInventoryMapper ecInventoryMapper;

    public List<EcListingLinkSkuInventoryVO> loadInventories(String skuCodes) {
        if (!StringUtils.hasText(skuCodes)) {
            return List.of();
        }
        List<String> codes = new ArrayList<>();
        for (String part : skuCodes.split(",")) {
            String code = part.trim();
            if (!code.isEmpty()) {
                codes.add(code);
            }
        }
        if (codes.isEmpty()) {
            return List.of();
        }

        Map<String, EcSku> skuMap = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                        .in(EcSku::getSkuCode, codes))
                .stream()
                .collect(Collectors.toMap(EcSku::getSkuCode, s -> s, (a, b) -> a, LinkedHashMap::new));

        Map<String, EcInventory> inventoryMap = ecInventoryMapper.selectList(new LambdaQueryWrapper<EcInventory>()
                        .in(EcInventory::getSkuCode, codes))
                .stream()
                .collect(Collectors.toMap(EcInventory::getSkuCode, i -> i, (a, b) -> a));

        Map<Long, EcProduct> productMap = skuMap.isEmpty() ? Map.of()
                : ecProductMapper.selectBatchIds(skuMap.values().stream()
                        .map(EcSku::getProductId)
                        .distinct()
                        .toList()).stream()
                .collect(Collectors.toMap(EcProduct::getId, p -> p, (a, b) -> a));

        List<EcListingLinkSkuInventoryVO> result = new ArrayList<>();
        for (String code : codes) {
            EcListingLinkSkuInventoryVO vo = new EcListingLinkSkuInventoryVO();
            vo.setSkuCode(code);
            EcSku sku = skuMap.get(code);
            if (sku != null) {
                vo.setSpecName(sku.getSpecName());
                vo.setSkuStatus(sku.getStatus());
                EcProduct product = productMap.get(sku.getProductId());
                vo.setInboundAllowed(isSkuAvailableForDisplay(sku, product));
            }
            EcInventory inventory = inventoryMap.get(code);
            if (inventory != null) {
                vo.setQuantity(inventory.getQuantity());
                vo.setAlertThreshold(inventory.getAlertThreshold());
                boolean ignoreAlert = inventory.getIgnoreAlert() != null && inventory.getIgnoreAlert() == 1;
                vo.setAlertActive(!ignoreAlert
                        && inventory.getAlertThreshold() != null
                        && inventory.getQuantity() != null
                        && inventory.getQuantity() <= inventory.getAlertThreshold());
            } else {
                vo.setQuantity(0);
                vo.setAlertActive(false);
            }
            result.add(vo);
        }
        return result;
    }

    private boolean isSkuAvailableForDisplay(EcSku sku, EcProduct product) {
        if (sku == null || !"ON_SALE".equals(sku.getStatus())) {
            return false;
        }
        return product != null && "ENABLED".equals(product.getStatus());
    }
}
