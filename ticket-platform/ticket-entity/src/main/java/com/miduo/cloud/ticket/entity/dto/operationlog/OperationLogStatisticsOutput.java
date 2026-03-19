package com.miduo.cloud.ticket.entity.dto.operationlog;

import lombok.Data;

import java.io.Serializable;

/**
 * 日志统计概览输出DTO
 * 接口编号：API000602
 * PRD §6.4 统计概览接口
 */
@Data
public class OperationLogStatisticsOutput implements Serializable {

    /** 今日操作总数 */
    private long todayTotalCount;

    /** 今日失败操作数 */
    private long todayFailureCount;

    /** 今日活跃操作人数 */
    private long todayActiveUserCount;

    /** 今日安全告警数（安全级日志次数） */
    private long todaySecurityAlertCount;
}
