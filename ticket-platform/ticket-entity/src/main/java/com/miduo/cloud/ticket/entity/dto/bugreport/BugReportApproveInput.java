package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;

/**
 * Bug简报审核通过入参
 */
@Data
public class BugReportApproveInput implements Serializable {

    /**
     * 审核通过意见（可选）
     */
    private String reviewComment;
}
