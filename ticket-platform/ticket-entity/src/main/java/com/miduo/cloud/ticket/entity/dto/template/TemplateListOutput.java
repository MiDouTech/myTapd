package com.miduo.cloud.ticket.entity.dto.template;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TemplateListOutput implements Serializable {

    private Long id;

    private String name;

    private Long categoryId;

    private String categoryName;

    private String fieldsConfig;

    private String description;

    private Integer isActive;

    private Date createTime;
}
