package com.miduo.cloud.ticket.entity.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 当前用户信息响应
 */
@Data
public class CurrentUserOutput implements Serializable {

    private Long id;
    private String name;
    private String employeeNo;
    private Long departmentId;
    private String departmentName;
    private String email;
    private String phone;
    private String position;
    private String avatarUrl;
    private String wecomUserid;
    private Integer accountStatus;
    private List<String> roleCodes;
    private Date createTime;
}
