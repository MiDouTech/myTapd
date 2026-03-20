package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_category")
public class TicketCategoryPO extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("parent_id")
    private Long parentId;

    @TableField("level")
    private Integer level;

    @TableField("path")
    private String path;

    @TableField("template_id")
    private Long templateId;

    @TableField("workflow_id")
    private Long workflowId;

    @TableField("sla_policy_id")
    private Long slaPolicyId;

    @TableField("default_group_id")
    private Long defaultGroupId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_active")
    private Integer isActive;

    @TableField("remark")
    private String remark;

    @TableField("nl_match_keywords")
    private String nlMatchKeywords;
}
