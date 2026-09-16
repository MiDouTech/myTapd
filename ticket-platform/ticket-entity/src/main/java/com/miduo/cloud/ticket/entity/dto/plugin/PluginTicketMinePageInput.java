package com.miduo.cloud.ticket.entity.dto.plugin;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * 插件我的工单分页
 * 接口编号：API000533
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PluginTicketMinePageInput extends PageInput {

    @Size(max = 100, message = "标题搜索关键字不能超过100个字符")
    private String title;

    @Min(value = 1, message = "分类ID必须大于0")
    private Long categoryId;

    @Size(max = 50, message = "状态不能超过50个字符")
    private String status;
}
