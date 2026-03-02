package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;

/**
 * Bug简报统计查询入参
 */
@Data
public class BugReportStatisticsInput implements Serializable {

    private String createTimeStart;

    private String createTimeEnd;
}
