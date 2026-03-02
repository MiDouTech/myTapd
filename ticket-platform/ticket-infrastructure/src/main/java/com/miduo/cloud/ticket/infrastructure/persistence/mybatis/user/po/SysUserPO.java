package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统用户PO - 映射 sys_user 表
 */
@Data
@TableName("sys_user")
public class SysUserPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("employee_no")
    private String employeeNo;

    @TableField("department_id")
    private Long departmentId;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("position")
    private String position;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("wecom_userid")
    private String wecomUserid;

    @TableField("account_status")
    private Integer accountStatus;

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
