package com.miduo.cloud.ticket.entity.dto.user;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePageInput extends PageInput {

    /**
     * 部门ID（可选）
     */
    private Long departmentId;

    /**
     * 关键字（姓名/工号）
     */
    private String keyword;

    /**
     * 账号状态（1:在职 2:停用 4:离职）
     */
    private Integer accountStatus;
}
