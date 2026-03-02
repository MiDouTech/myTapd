package com.miduo.cloud.ticket.entity.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户列表查询参数
 */
@Data
public class UserListInput implements Serializable {

    /**
     * 部门ID（可选筛选）
     */
    private Long departmentId;

    /**
     * 关键字（姓名/工号模糊搜索）
     */
    private String keyword;

    /**
     * 账号状态筛选
     */
    private Integer accountStatus;
}
