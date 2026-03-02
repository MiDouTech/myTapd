package com.miduo.cloud.ticket.controller.user;

import com.miduo.cloud.ticket.application.user.UserApplicationService;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.user.CurrentUserOutput;
import com.miduo.cloud.ticket.entity.dto.user.UserListInput;
import com.miduo.cloud.ticket.entity.dto.user.UserListOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口
 */
@Tag(name = "用户管理", description = "用户查询")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /**
     * 获取当前用户信息
     * 接口编号：API000402
     * 产品文档功能：4.10.1 用户管理
     */
    @Operation(summary = "获取当前用户信息", description = "接口编号：API000402。获取当前登录用户的详细信息")
    @GetMapping("/current")
    public ApiResult<CurrentUserOutput> getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        CurrentUserOutput output = userApplicationService.getCurrentUser(userId);
        return ApiResult.success(output);
    }

    /**
     * 用户列表（按部门筛选）
     * 接口编号：API000403
     * 产品文档功能：4.10.1 用户管理
     */
    @Operation(summary = "用户列表", description = "接口编号：API000403。按部门筛选用户列表")
    @GetMapping("/list")
    public ApiResult<List<UserListOutput>> getUserList(UserListInput input) {
        List<UserListOutput> list = userApplicationService.getUserList(input);
        return ApiResult.success(list);
    }
}
