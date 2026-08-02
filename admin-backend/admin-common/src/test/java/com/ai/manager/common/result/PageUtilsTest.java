package com.ai.manager.common.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageUtils 分页参数兜底与 PageResult 组装断言。
 */
class PageUtilsTest {

    @Test
    void normalizePage_withNull_shouldReturnDefault() {
        assertThat(PageUtils.normalizePage(null)).isEqualTo(PageUtils.DEFAULT_PAGE);
    }

    @Test
    void normalizePage_withLessThanOne_shouldReturnDefault() {
        assertThat(PageUtils.normalizePage(0L)).isEqualTo(PageUtils.DEFAULT_PAGE);
        assertThat(PageUtils.normalizePage(-5L)).isEqualTo(PageUtils.DEFAULT_PAGE);
    }

    @Test
    void normalizePage_withValidValue_shouldKeepAsIs() {
        assertThat(PageUtils.normalizePage(3L)).isEqualTo(3L);
    }

    @Test
    void normalizePageSize_withNull_shouldReturnDefault() {
        assertThat(PageUtils.normalizePageSize(null)).isEqualTo(PageUtils.DEFAULT_PAGE_SIZE);
    }

    @Test
    void normalizePageSize_withLessThanOne_shouldReturnDefault() {
        assertThat(PageUtils.normalizePageSize(0L)).isEqualTo(PageUtils.DEFAULT_PAGE_SIZE);
    }

    @Test
    void normalizePageSize_withOverMax_shouldCapAtMax() {
        assertThat(PageUtils.normalizePageSize(1000L)).isEqualTo(PageUtils.MAX_PAGE_SIZE);
    }

    @Test
    void normalizePageSize_withValidValue_shouldKeepAsIs() {
        assertThat(PageUtils.normalizePageSize(30L)).isEqualTo(30L);
    }

    @Test
    void of_withRecords_shouldBuildPageResult() {
        PageResult<String> result = PageUtils.of(List.of("a", "b"), 2, 1, 10);

        assertThat(result.getRecords()).containsExactly("a", "b");
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
    }

    @Test
    void of_withMapper_shouldMapRecords() {
        PageResult<String> result = PageUtils.of(
                List.of("x", "y"), 2, 1, 10, String::toUpperCase);

        assertThat(result.getRecords()).containsExactly("X", "Y");
        assertThat(result.getTotal()).isEqualTo(2);
    }
}
