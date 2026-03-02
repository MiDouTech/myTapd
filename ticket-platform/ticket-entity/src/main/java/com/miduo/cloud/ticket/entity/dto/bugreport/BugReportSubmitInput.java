package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;

/**
 * Bug简报提交审核入参
 */
@Data
public class BugReportSubmitInput implements Serializable {

    /**
     * 可选：提交时指定审核人
     */
    private Long reviewerId;

    private String remark;
}
