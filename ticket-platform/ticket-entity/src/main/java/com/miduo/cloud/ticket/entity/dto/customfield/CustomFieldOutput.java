package com.miduo.cloud.ticket.entity.dto.customfield;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class CustomFieldOutput implements Serializable {

    private Long id;
    private String fieldCode;
    private String fieldTitle;
    private String fieldType;
    private String fieldTypeLabel;
    private String description;
    private String placeholder;
    private Integer defaultRequired;
    private String defaultValue;
    private Integer maxLength;
    private List<CustomFieldOptionInput> options;
    private Integer isActive;
    private Integer referenceCount;
    private Date createTime;
    private Date updateTime;
}
