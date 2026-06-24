package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 插件 SDK 初始化配置
 * 接口编号：API000536
 */
@Data
public class PluginConfigOutput implements Serializable {

    private String appName;

    private String systemCode;

    private String defaultPriority;

    private Boolean showPriorityPicker;

    private Map<String, Object> theme;
}
