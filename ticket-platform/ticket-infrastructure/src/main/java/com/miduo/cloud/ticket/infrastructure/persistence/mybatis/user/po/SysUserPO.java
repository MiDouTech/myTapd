package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUserPO extends BaseEntity {

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
}
