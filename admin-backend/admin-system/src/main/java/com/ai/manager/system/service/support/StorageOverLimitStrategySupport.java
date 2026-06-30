package com.ai.manager.system.service.support;

import org.springframework.util.StringUtils;

public final class StorageOverLimitStrategySupport {

    public static final String REJECT = "REJECT";
    public static final String CLEANUP_OLDEST = "CLEANUP_OLDEST";
    public static final String CLEANUP_LARGEST = "CLEANUP_LARGEST";

    public static final String ZONE_LOCAL_TOTAL = "LOCAL_TOTAL";
    public static final String ZONE_REDIS_CACHE = "REDIS_CACHE";

    private StorageOverLimitStrategySupport() {
    }

    public static boolean isValid(String strategy) {
        if (!StringUtils.hasText(strategy)) {
            return false;
        }
        String normalized = strategy.trim().toUpperCase();
        return REJECT.equals(normalized)
                || CLEANUP_OLDEST.equals(normalized)
                || CLEANUP_LARGEST.equals(normalized);
    }

    public static String normalize(String strategy, String fallback) {
        if (isValid(strategy)) {
            return strategy.trim().toUpperCase();
        }
        return isValid(fallback) ? fallback.trim().toUpperCase() : REJECT;
    }

    public static boolean isCleanup(String strategy) {
        String normalized = normalize(strategy, REJECT);
        return CLEANUP_OLDEST.equals(normalized) || CLEANUP_LARGEST.equals(normalized);
    }
}
