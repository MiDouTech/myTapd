package com.miduo.cloud.ticket.entity.dto.template;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TemplateFieldOutput implements Serializable {
    private String fieldSource;
    private Long customFieldId;
    private String fieldCode;
    private String fieldTitle;
    private String fieldType;
    private String fieldTypeLabel;
    private Integer sortOrder;
    private Integer isEnabled;
    private Integer isRequired;
    private String placeholder;
    private String defaultValue;
    private List<TemplateFieldOptionOutput> options;
    private Boolean deletable;
    private Boolean definitionActive;
}
