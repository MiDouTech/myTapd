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
