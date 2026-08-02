package com.ai.manager.system.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyChecklistSaveRequest {

    /** 清单所属日期 */
    @NotNull(message = "日期不能为空")
    private LocalDate date;

    /** 清单明细，至少一条 */
    @Valid
    @NotNull(message = "清单明细不能为空")
    @Size(min = 1, message = "清单明细至少一条")
    private List<DailyChecklistItemRequest> items;

    @Data
    public static class DailyChecklistItemRequest {
        /** 条目键，唯一标识某阶段条目 */
        @NotBlank(message = "条目键不能为空")
        private String itemKey;

        /** 完成标记：0 未完成 / 1 已完成 */
        @NotNull(message = "完成标记不能为空")
        private Integer completed;

        private String content;
    }
}
