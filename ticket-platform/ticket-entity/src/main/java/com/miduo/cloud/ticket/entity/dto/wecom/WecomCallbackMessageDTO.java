package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.io.Serializable;

/**
 * 企微回调消息异步载荷
 */
@Data
public class WecomCallbackMessageDTO implements Serializable {

    /**
     * 消息ID（用于幂等）
     */
    private String msgId;

    /**
     * 消息类型（text/event）
     */
    private String msgType;

    /**
     * 来源群ChatID
     */
    private String chatId;

    /**
     * 发送人企微UserID
     */
    private String fromWecomUserid;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 企微图片MediaId（MsgType=image时有值）
     */
    private String mediaId;

    /**
     * 企微图片临时预览URL（MsgType=image时有值）
     */
    private String picUrl;

    /**
     * 解密后的原始XML
     */
    private String rawXml;

    /**
     * 消息时间戳
     */
    private String createTime;

    /**
     * 企微AI bot回调消息的response_url，用于直接回复消息（无需access_token）
     */
    private String responseUrl;

    /**
     * 聊天类型（single=单聊，group=群聊），企微AI bot消息携带
     */
    private String chatType;
}
