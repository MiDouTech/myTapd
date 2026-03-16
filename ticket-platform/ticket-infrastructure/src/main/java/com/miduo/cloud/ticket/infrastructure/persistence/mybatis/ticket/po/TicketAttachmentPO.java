package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_attachment")
public class TicketAttachmentPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField("source")
    private String source;

    @TableField("wecom_msg_id")
    private String wecomMsgId;

    @TableField("file_type")
    private String fileType;

    @TableField("uploaded_by")
    private Long uploadedBy;
}
