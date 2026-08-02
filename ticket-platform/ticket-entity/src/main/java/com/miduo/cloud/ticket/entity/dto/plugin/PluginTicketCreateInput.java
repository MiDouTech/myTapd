package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 插件创建工单请求
 * 接口编号：API000532
 */
@Data
public class PluginTicketCreateInput implements Serializable {

    @NotBlank(message = "问题描述不能为空")
    private String description;

    /** 客户端根据问题描述自动生成；为空时仍兼容服务端历史标题生成逻辑。 */
    private String title;

    /** 客户端组装的分类前缀与描述正文，description 为兼容旧版 SDK 保留。 */
    private String content;

    private String priority;

    private String externalTicketRef;

    private Map<String, Object> pluginContext;

    private List<String> attachments;

    private Map<String, String> customFields;
}
