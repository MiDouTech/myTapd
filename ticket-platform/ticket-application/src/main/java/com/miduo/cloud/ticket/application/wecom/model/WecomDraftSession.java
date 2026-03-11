package com.miduo.cloud.ticket.application.wecom.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 企微工单草稿会话模型
 * Task023：企微文本消息自动创建工单 - 会话状态管理
 */
@Data
public class WecomDraftSession implements Serializable {

    /**
     * 会话步骤枚举
     */
    public enum Step {
        /**
         * 等待用户确认预览内容
         */
        PENDING_CONFIRM,
        /**
         * 等待用户选择修改分类
         */
        MODIFY_CATEGORY,
        /**
         * 等待用户选择修改优先级
         */
        MODIFY_PRIORITY,
        /**
         * 等待用户补充描述
         */
        SUPPLEMENT_DESC
    }

    /**
     * 来源 chatId 或 wecomUserId（区分群聊/私聊）
     */
    private String sessionKey;

    /**
     * 发送人企微UserId
     */
    private String wecomUserId;

    /**
     * 当前步骤
     */
    private Step step;

    /**
     * 解析出的工单标题
     */
    private String title;

    /**
     * 解析出的分类路径
     */
    private String categoryPath;

    /**
     * 解析出的优先级
     */
    private String priority;

    /**
     * 描述内容
     */
    private String description;

    /**
     * NLU置信度
     */
    private Integer nlpConfidence;

    /**
     * 来源 chatId（群聊场景）
     */
    private String chatId;

    /**
     * 是否为群聊
     */
    private boolean groupChat;
}
