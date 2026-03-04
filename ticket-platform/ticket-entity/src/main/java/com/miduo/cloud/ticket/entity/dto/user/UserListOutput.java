package com.miduo.cloud.ticket.entity.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户列表输出项
 */
@Data
public class UserListOutput implements Serializable {

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
    private Integer accountStatus;
    private List<String> roleCodes;
    private Date createTime;
}
