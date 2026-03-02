package com.miduo.cloud.ticket.entity.dto.bugreport;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Bug简报分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BugReportPageInput extends PageInput implements Serializable {

    private String reportNo;

    private String status;

    private String defectCategory;

    private Long reviewerId;

    private Long responsibleUserId;

    private Long ticketId;

    private String createTimeStart;

    private String createTimeEnd;
}
