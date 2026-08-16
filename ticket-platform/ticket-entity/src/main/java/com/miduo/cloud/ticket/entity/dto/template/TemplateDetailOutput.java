package com.miduo.cloud.ticket.entity.dto.template;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class TemplateDetailOutput implements Serializable {
    private Long id;
    private String templateCode;
    private String name;
    private String description;
    private Integer isSystem;
    private Integer isActive;
    private Integer fieldCount;
    private Date createTime;
    private Date updateTime;
    private List<TemplateFieldOutput> fields;
}
