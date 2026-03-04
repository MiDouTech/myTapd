package com.miduo.cloud.ticket.entity.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工分页输出
 */
@Data
public class EmployeePageOutput implements Serializable {

    private Long id;
    private String name;
    private String employeeNo;
    private Long departmentId;
    private String departmentName;
    private String emailMasked;
    private String phoneMasked;
    private String position;
    private Integer gender;
    private String genderName;
    private String avatarUrl;
    private String wecomUseridMasked;
    private Integer accountStatus;
    private String accountStatusName;
    private Integer syncStatus;
    private String syncStatusName;
    private Date syncTime;
    private Date createTime;
}
