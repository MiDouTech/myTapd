package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

/**
 * 图片上传响应 DTO
 */
@Data
public class ImageUploadOutput {

    /**
     * 图片可访问 URL
     */
    private String url;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件 MIME 类型
     */
    private String fileType;
}
