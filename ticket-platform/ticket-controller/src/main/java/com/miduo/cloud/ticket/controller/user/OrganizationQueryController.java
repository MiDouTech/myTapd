package com.miduo.cloud.ticket.controller.user;

import com.miduo.cloud.ticket.application.user.DepartmentApplicationService;
import com.miduo.cloud.ticket.application.user.UserApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.entity.dto.user.DepartmentTreeOutput;
import com.miduo.cloud.ticket.entity.dto.user.EmployeeDetailOutput;
import com.miduo.cloud.ticket.entity.dto.user.EmployeePageInput;
import com.miduo.cloud.ticket.entity.dto.user.EmployeePageOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * SSO一期组织查询接口
 */
@Tag(name = "组织查询V1", description = "部门树、员工分页、员工详情")
@RestController
@RequestMapping("/api/v1")
public class OrganizationQueryController {

    private final DepartmentApplicationService departmentApplicationService;
    private final UserApplicationService userApplicationService;

    public OrganizationQueryController(DepartmentApplicationService departmentApplicationService,
                                       UserApplicationService userApplicationService) {
        this.departmentApplicationService = departmentApplicationService;
        this.userApplicationService = userApplicationService;
    }

    /**
     * 部门树查询
     * 接口编号：API000428
     * 产品文档功能：SSO一期-组织查询-部门树
     */
    @Operation(summary = "部门树查询", description = "接口编号：API000428")
    @GetMapping("/departments/tree")
    public ApiResult<List<DepartmentTreeOutput>> getDepartmentsTree() {
        return ApiResult.success(departmentApplicationService.getDepartmentTree());
    }

    /**
     * 员工分页查询
     * 接口编号：API000429
     * 产品文档功能：SSO一期-组织查询-员工分页
     */
    @Operation(summary = "员工分页查询", description = "接口编号：API000429")
    @GetMapping("/employees/page")
    public ApiResult<PageOutput<EmployeePageOutput>> pageEmployees(@Valid EmployeePageInput input) {
        return ApiResult.success(userApplicationService.pageEmployees(input));
    }

    /**
     * 员工详情查询
     * 接口编号：API000430
     * 产品文档功能：SSO一期-组织查询-员工详情
     */
    @Operation(summary = "员工详情查询", description = "接口编号：API000430")
    @GetMapping("/employees/detail/{id}")
    public ApiResult<EmployeeDetailOutput> getEmployeeDetail(@PathVariable Long id) {
        return ApiResult.success(userApplicationService.getEmployeeDetail(id));
    }
}
