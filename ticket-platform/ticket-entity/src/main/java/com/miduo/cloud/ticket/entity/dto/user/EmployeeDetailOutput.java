package com.miduo.cloud.ticket.entity.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 员工详情输出
 */
@Data
public class EmployeeDetailOutput implements Serializable {

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
    private List<String> roleCodes;
    private Date createTime;
}
