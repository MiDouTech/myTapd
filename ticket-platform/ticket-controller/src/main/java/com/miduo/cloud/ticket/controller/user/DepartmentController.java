package com.miduo.cloud.ticket.controller.user;

import com.miduo.cloud.ticket.application.user.DepartmentApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.user.DepartmentTreeOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门接口
 */
@Tag(name = "部门管理", description = "组织架构")
@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    private final DepartmentApplicationService departmentApplicationService;

    public DepartmentController(DepartmentApplicationService departmentApplicationService) {
        this.departmentApplicationService = departmentApplicationService;
    }

    /**
     * 组织架构树
     * 接口编号：API000404
     * 产品文档功能：4.10.1 组织架构与用户管理
     */
    @Operation(summary = "组织架构树", description = "接口编号：API000404。获取部门树形结构")
    @GetMapping("/tree")
    public ApiResult<List<DepartmentTreeOutput>> getDepartmentTree() {
        List<DepartmentTreeOutput> tree = departmentApplicationService.getDepartmentTree();
        return ApiResult.success(tree);
    }
}
