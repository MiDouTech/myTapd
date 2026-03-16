package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.util.Date;

/**
 * NLP解析日志分页查询响应
 * Task023：企微自然语言建单 - 解析日志
 */
@Data
public class NlpLogPageOutput {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 来源群ChatID
     */
    private String chatId;

    /**
     * 企微消息ID
     */
    private String msgId;

    /**
     * 发送人企微UserID
     */
    private String fromWecomUserid;

    /**
     * 原始消息内容
     */
    private String rawMessage;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 解析类型：template=格式模板 natural_language=自然语言
     */
    private String parseType;

    /**
     * NLU解析置信度(0-100)
     */
    private Integer nlpConfidence;

    /**
     * 关联工单ID
     */
    private Long ticketId;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private Date createTime;
}
