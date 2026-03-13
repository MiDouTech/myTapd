package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 工单附件保存请求 DTO
 */
@Data
public class TicketAttachmentSaveInput {

    /**
     * 工单ID
     */
    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件访问URL（七牛云返回的完整URL）
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件 MIME 类型
     */
    private String fileType;
}
