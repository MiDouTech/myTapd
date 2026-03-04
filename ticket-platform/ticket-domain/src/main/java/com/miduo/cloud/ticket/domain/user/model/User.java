package com.miduo.cloud.ticket.domain.user.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户领域模型
 */
@Data
public class User implements Serializable {

    private Long id;
    private String name;
    private String employeeNo;
    private Long departmentId;
    private String departmentName;
    private String email;
    private String phone;
    private String position;
    private Integer gender;
    private String avatarUrl;
    private String wecomUserid;
    private Integer accountStatus;
    private Integer syncStatus;
    private Date syncTime;
    private List<String> roleCodes;
    private Date createTime;
    private Date updateTime;
}
