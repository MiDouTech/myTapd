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
     * 企微智能机器人加密资源下载 URL：图片为 image.url，视频为 video.url（与图片相同，需 callbackAesKey 解密）
     */
    private String downloadUrl;

    /**
     * 企微AI bot 历史字段（当前智能机器人图片/视频统一用回调 AESKey 解密，此字段通常为空）
     */
    private String aesKey;

    /**
     * 保留字段；智能机器人视频回调无独立缩略图 MediaId
     */
    private String thumbMediaId;

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
