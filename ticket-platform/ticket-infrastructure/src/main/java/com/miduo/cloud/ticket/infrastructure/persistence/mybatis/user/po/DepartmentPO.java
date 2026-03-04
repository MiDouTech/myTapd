package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 部门PO - 映射 department 表
 */
@Data
@TableName("department")
public class DepartmentPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("parent_id")
    private Long parentId;

    @TableField("wecom_dept_id")
    private Long wecomDeptId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("dept_status")
    private Integer deptStatus;

    @TableField("sync_status")
    private Integer syncStatus;

    @TableField("sync_time")
    private Date syncTime;

    @TableField("leader_wecom_userid")
    private String leaderWecomUserid;

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
