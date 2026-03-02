package com.miduo.cloud.ticket.controller.workflow;

import com.miduo.cloud.ticket.application.workflow.HandlerGroupAppService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.workflow.HandlerGroupCreateInput;
import com.miduo.cloud.ticket.entity.dto.workflow.HandlerGroupListOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 处理组管理接口
 */
@Tag(name = "处理组管理", description = "处理组CRUD、成员管理")
@RestController
@RequestMapping("/api/handler-group")
public class HandlerGroupController {

    private final HandlerGroupAppService handlerGroupAppService;

    public HandlerGroupController(HandlerGroupAppService handlerGroupAppService) {
        this.handlerGroupAppService = handlerGroupAppService;
    }

    /**
     * 处理组列表
     * 接口编号：API000018
     * 产品文档功能：4.5 分派与路由 - 处理组管理
     */
    @Operation(summary = "处理组列表", description = "接口编号：API000018")
    @GetMapping("/list")
    public ApiResult<List<HandlerGroupListOutput>> listHandlerGroups() {
        List<HandlerGroupListOutput> result = handlerGroupAppService.listHandlerGroups();
        return ApiResult.success(result);
    }

    /**
     * 新增处理组
     * 接口编号：API000019
     * 产品文档功能：4.5 分派与路由 - 处理组创建
     */
    @Operation(summary = "新增处理组", description = "接口编号：API000019")
    @PostMapping("/create")
    public ApiResult<Long> createHandlerGroup(@Valid @RequestBody HandlerGroupCreateInput input) {
        Long groupId = handlerGroupAppService.createHandlerGroup(input);
        return ApiResult.success(groupId);
    }
}
