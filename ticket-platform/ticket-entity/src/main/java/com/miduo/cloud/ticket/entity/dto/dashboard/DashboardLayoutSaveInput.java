package com.miduo.cloud.ticket.entity.dto.dashboard;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 保存仪表盘布局请求DTO
 */
@Data
public class DashboardLayoutSaveInput {

    /**
     * 布局列表，不能为空
     */
    @NotNull(message = "布局列表不能为空")
    @NotEmpty(message = "布局列表不能为空")
    @Valid
    private List<LayoutItem> layouts;

    /**
     * 布局配置项
     */
    @Data
    public static class LayoutItem {

        /**
         * 行组Key
         */
        @NotBlank(message = "行组Key不能为空")
        private String rowGroupKey;

        /**
         * 排列序号
         */
        @NotNull(message = "排列序号不能为空")
        @Min(value = 0, message = "排列序号不能小于0")
        private Integer sortOrder;
    }
}
