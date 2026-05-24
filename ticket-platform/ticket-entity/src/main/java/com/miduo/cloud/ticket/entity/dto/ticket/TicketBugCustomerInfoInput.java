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

    /**
     * 手工有效报告（YES/NO），仅允许在工单已关闭时修改
     */
    private String manualValidReport;
}
