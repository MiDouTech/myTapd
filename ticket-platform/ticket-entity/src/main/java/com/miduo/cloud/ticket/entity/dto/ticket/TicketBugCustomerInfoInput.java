package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

/**
 * 缺陷工单-客服信息更新入参
 */
@Data
public class TicketBugCustomerInfoInput implements Serializable {

    private String merchantNo;

    private String companyName;

    private String merchantAccount;

    private String problemDesc;

    private String expectedResult;

    private String sceneCode;

    private String problemScreenshot;

    /** 请求路径或 URL（保存前由服务端脱敏敏感 query） */
    private String troubleshootRequestUrl;

    private String troubleshootHttpStatus;

    private String troubleshootBizErrorCode;

    private String troubleshootTraceId;

    /** ISO 日期时间字符串，如 2026-03-31T12:00:00 */
    private String troubleshootOccurredAt;

    /** H5 / MINI_APP / APP / PC / UNKNOWN */
    private String troubleshootClientType;
}
