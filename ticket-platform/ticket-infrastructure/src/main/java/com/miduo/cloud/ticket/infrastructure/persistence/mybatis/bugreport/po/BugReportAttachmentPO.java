package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Bug简报附件PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bug_report_attachment")
public class BugReportAttachmentPO extends BaseEntity {

    @TableField("report_id")
    private Long reportId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField("uploaded_by")
    private Long uploadedBy;
}
