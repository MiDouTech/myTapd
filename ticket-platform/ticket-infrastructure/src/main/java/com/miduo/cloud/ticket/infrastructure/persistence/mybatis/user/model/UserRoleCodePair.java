package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户角色编码映射
 */
@Data
public class UserRoleCodePair implements Serializable {

    private Long userId;
    private String roleCode;
}

