package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import java.io.Serializable;

@Data
public class PluginCategoryConfigOutput implements Serializable {
    private Long id;
    private String name;
    private Long templateId;
    private String templateName;
    private String fieldsConfig;
}
