package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 企微图片消息暂存 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_pending_image")
public class WecomPendingImagePO extends BaseEntity {

    @TableField("chat_id")
    private String chatId;

    @TableField("from_user_id")
    private String fromUserId;

    @TableField("msg_id")
    private String msgId;

    @TableField("media_id")
    private String mediaId;

    @TableField("pic_url")
    private String picUrl;

    @TableField("qiniu_url")
    private String qiniuUrl;

    /**
     * 媒体类型：image / video
     */
    @TableField("media_type")
    private String mediaType;

    /**
     * 视频缩略图七牛URL（仅视频消息有值）
     */
    @TableField("thumb_url")
    private String thumbUrl;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("status")
    private String status;

    @TableField("expire_time")
    private Date expireTime;
}
