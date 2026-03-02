package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 缺陷工单客服信息PO
 * 缺陷工单客服信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_bug_info")
public class TicketBugInfoPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("merchant_no")
    private String merchantNo;

    @TableField("company_name")
    private String companyName;

    @TableField("merchant_account")
    private String merchantAccount;

    @TableField("problem_desc")
    private String problemDesc;

    @TableField("expected_result")
    private String expectedResult;

    @TableField("scene_code")
    private String sceneCode;

    @TableField("problem_screenshot")
    private String problemScreenshot;
}
