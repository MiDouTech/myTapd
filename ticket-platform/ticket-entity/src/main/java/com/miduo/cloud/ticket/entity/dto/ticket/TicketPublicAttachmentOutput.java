package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

/**
 * 工单公开详情-附件（只读）
 */
@Data
public class TicketPublicAttachmentOutput implements Serializable {

    private Long id;

    private String fileName;

    private String fileType;

    private Long fileSize;

    /** 访问地址（与登录态一致，通常为 CDN 直链） */
    private String fileUrl;
}
