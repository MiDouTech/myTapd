package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企微机器人消息日志持久化对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_bot_message_log")
public class WecomBotMessageLogPO extends BaseEntity {

    /**
     * 来源群ChatID
     */
    @TableField("chat_id")
    private String chatId;

    /**
     * 企微消息ID（用于去重）
     */
    @TableField("msg_id")
    private String msgId;

    /**
     * 发送人企微UserID
     */
    @TableField("from_wecom_userid")
    private String fromWecomUserid;

    /**
     * 原始消息内容
     */
    @TableField("raw_message")
    private String rawMessage;

    /**
     * 解析结果（JSON）
     */
    @TableField("parsed_result")
    private String parsedResult;

    /**
     * 关联工单ID
     */
    @TableField("ticket_id")
    private Long ticketId;

    /**
     * 处理状态（SUCCESS/FAIL/DUPLICATE）
     */
    @TableField("status")
    private String status;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;
}
