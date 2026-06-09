package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.EcListingLink;
import com.ai.manager.system.domain.entity.EcListingLinkSku;
import com.ai.manager.system.mapper.EcListingLinkMapper;
import com.ai.manager.system.mapper.EcListingLinkSkuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EcSalesOrderMatchSupport {

    private final EcListingLinkMapper ecListingLinkMapper;
    private final EcListingLinkSkuMapper ecListingLinkSkuMapper;

    public LinkSkuMatchResult matchLinkSku(Long shopId, String linkName, String skuSpecName) {
        if (shopId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择店铺");
        }
        if (!StringUtils.hasText(linkName)) {
            return LinkSkuMatchResult.unmatched("链接名称不能为空");
        }
        String ln = linkName.trim();
        List<EcListingLink> links = ecListingLinkMapper.selectList(new LambdaQueryWrapper<EcListingLink>()
                .eq(EcListingLink::getShopId, shopId)
                .eq(EcListingLink::getName, ln));
        if (links.isEmpty()) {
            return LinkSkuMatchResult.unmatched("未找到链接：" + ln);
        }
        if (links.size() > 1) {
            return LinkSkuMatchResult.unmatched("同店铺存在多个同名链接：" + ln);
        }
        EcListingLink link = links.get(0);
        if (!StringUtils.hasText(skuSpecName)) {
            return matchSingleSkuUnderLink(link);
        }
        String sn = skuSpecName.trim();
        List<EcListingLinkSku> skus = ecListingLinkSkuMapper.selectList(new LambdaQueryWrapper<EcListingLinkSku>()
                .eq(EcListingLinkSku::getLinkId, link.getId())
                .eq(EcListingLinkSku::getSkuName, sn));
        if (skus.isEmpty()) {
            return LinkSkuMatchResult.unmatched("链接「" + ln + "」下未找到 SKU：" + sn);
        }
        if (skus.size() > 1) {
            return LinkSkuMatchResult.unmatched("链接「" + ln + "」下存在多个同名 SKU：" + sn);
        }
        return buildMatchResult(link, skus.get(0));
    }

    private LinkSkuMatchResult matchSingleSkuUnderLink(EcListingLink link) {
        List<EcListingLinkSku> skus = ecListingLinkSkuMapper.selectList(new LambdaQueryWrapper<EcListingLinkSku>()
                .eq(EcListingLinkSku::getLinkId, link.getId()));
        if (skus.isEmpty()) {
            return LinkSkuMatchResult.unmatched("链接「" + link.getName() + "」下无 SKU");
        }
        if (skus.size() > 1) {
            return LinkSkuMatchResult.unmatched(
                    "链接「" + link.getName() + "」下存在多个 SKU，请填写规格名称以区分");
        }
        return buildMatchResult(link, skus.get(0));
    }

    private LinkSkuMatchResult buildMatchResult(EcListingLink link, EcListingLinkSku sku) {
        LinkSkuMatchResult result = new LinkSkuMatchResult();
        result.setMatched(true);
        result.setListingLinkId(link.getId());
        result.setListingLinkSkuId(sku.getId());
        result.setLinkName(link.getName());
        result.setSkuSpecName(sku.getSkuName());
        result.setSkuCodes(sku.getSkuCodes());
        result.setListingLinkSku(sku);
        return result;
    }

    public LinkSkuMatchResult matchByListingLinkSkuId(Long listingLinkSkuId) {
        if (listingLinkSkuId == null) {
            return LinkSkuMatchResult.unmatched("未指定链接 SKU");
        }
        EcListingLinkSku sku = ecListingLinkSkuMapper.selectById(listingLinkSkuId);
        if (sku == null) {
            return LinkSkuMatchResult.unmatched("链接 SKU 不存在");
        }
        EcListingLink link = ecListingLinkMapper.selectById(sku.getLinkId());
        if (link == null) {
            return LinkSkuMatchResult.unmatched("上架链接不存在");
        }
        return buildMatchResult(link, sku);
    }

    @Data
    public static class LinkSkuMatchResult {
        private boolean matched;
        private String message;
        private Long listingLinkId;
        private Long listingLinkSkuId;
        private String linkName;
        private String skuSpecName;
        private String skuCodes;
        private EcListingLinkSku listingLinkSku;

        public static LinkSkuMatchResult unmatched(String message) {
            LinkSkuMatchResult r = new LinkSkuMatchResult();
            r.setMatched(false);
            r.setMessage(message);
            return r;
        }
    }
}
