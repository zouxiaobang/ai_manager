package com.ai.manager.common.result;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PageUtils {

    public static final long DEFAULT_PAGE = 1L;
    public static final long DEFAULT_PAGE_SIZE = 20L;
    public static final long MAX_PAGE_SIZE = 100L;

    private PageUtils() {
    }

    public static long normalizePage(Long page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    public static long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }

    public static <E, V> PageResult<V> of(List<E> records, long total, long page, long pageSize, Function<E, V> mapper) {
        List<V> mapped = records.stream().map(mapper).collect(Collectors.toList());
        return of(mapped, total, page, pageSize);
    }
}
