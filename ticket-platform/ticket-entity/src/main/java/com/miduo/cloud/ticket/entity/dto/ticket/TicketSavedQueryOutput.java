package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class TicketSavedQueryOutput {
    private Long id;
    private String name;
    private Map<String, Object> queryConfig;
    private List<String> exportFields;
    private Integer schemaVersion;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
}
