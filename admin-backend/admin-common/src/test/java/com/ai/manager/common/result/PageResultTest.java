package com.ai.manager.common.result;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageResult 默认值及 empty 工厂方法断言。
 */
class PageResultTest {

    @Test
    void defaultRecords_shouldBeEmptyList() {
        PageResult<String> result = new PageResult<>();

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getExtra()).isNull();
    }

    @Test
    void empty_shouldBuildZeroTotalResult() {
        PageResult<String> result = PageResult.empty(3, 20);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getPage()).isEqualTo(3);
        assertThat(result.getPageSize()).isEqualTo(20);
    }

    @Test
    void setters_shouldAllowFullConstruction() {
        PageResult<String> result = new PageResult<>();
        result.setRecords(List.of("a", "b"));
        result.setTotal(2);
        result.setPage(1);
        result.setPageSize(10);
        result.setExtra(Map.of("sum", 42));

        assertThat(result.getRecords()).containsExactly("a", "b");
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getExtra()).containsEntry("sum", 42);
    }
}
