package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Bug简报分页项
 */
@Data
public class BugReportPageOutput implements Serializable {

    private Long id;

    private String reportNo;

    /** 问题描述，列表中作为标题展示 */
    private String problemDesc;

    private String status;

    private String statusLabel;

    private String defectCategory;

    private String severityLevel;

    private Long reviewerId;

    private String reviewerName;

    private Date submittedAt;

    private Date createTime;

    private Date updateTime;
}
