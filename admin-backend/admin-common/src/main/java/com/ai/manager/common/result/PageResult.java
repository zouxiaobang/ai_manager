package com.ai.manager.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records = Collections.emptyList();

    private long total;

    private long page;

    private long pageSize;

    /** 扩展统计字段，如库存合计 */
    private Map<String, Object> extra;

    public static <T> PageResult<T> empty(long page, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(Collections.emptyList());
        result.setTotal(0);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }
}
