package com.miduo.cloud.ticket.entity.dto.template;

import lombok.Data;

import java.io.Serializable;

@Data
public class TemplateFieldOptionOutput implements Serializable {
    private String label;
    private String value;
    private Integer sortOrder;
    private Integer enabled;
}
