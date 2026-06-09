package com.ai.manager.system.job;

import com.ai.manager.system.service.EcListingLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EcListingLinkPricingRecalcJob {

    private final EcListingLinkService ecListingLinkService;

    /** 每天凌晨 2 点批量重算上架链接定价 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void recalculateAllPricing() {
        log.info("开始执行上架链接定价定时重算");
        int updated = ecListingLinkService.recalculateAllPricing();
        log.info("上架链接定价定时重算结束，更新 {} 条 SKU", updated);
    }
}
