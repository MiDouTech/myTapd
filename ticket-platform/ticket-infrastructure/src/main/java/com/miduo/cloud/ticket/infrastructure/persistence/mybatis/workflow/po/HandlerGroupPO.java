package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 处理组PO
 */
@Data
@TableName("handler_group")
public class HandlerGroupPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("leader_id")
    private Long leaderId;

    @TableField("description")
    private String description;

    @TableField("skill_tags")
    private String skillTags;

    @TableField("is_active")
    private Integer isActive;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
