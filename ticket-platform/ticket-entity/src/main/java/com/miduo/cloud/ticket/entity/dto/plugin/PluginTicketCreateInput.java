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

    private String priority;

    private String externalTicketRef;

    private Map<String, Object> pluginContext;

    private List<String> attachments;

    private Map<String, String> customFields;
}
