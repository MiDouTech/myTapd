package com.miduo.cloud.ticket.application.webhook;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bug 简报归档 Webhook 负载（企微机器人 text 适配在 {@link WebhookDispatchService} 中组装）
 */
@Data
public class BugReportArchivedWebhookPayload implements Serializable {

    private Long reportId;

    private String reportNo;

    /**
     * 与企微群 @ 顺序一致：反馈人、工单创建人/处理人、简报责任人（去重前顺序由业务层保证）
     */
    private List<Long> mentionedUserIds = new ArrayList<>();

    /**
     * 简报字段多行纯文本（不含 Markdown 引用前缀，便于企微 text 展示）
     */
    private String detailText;
}
