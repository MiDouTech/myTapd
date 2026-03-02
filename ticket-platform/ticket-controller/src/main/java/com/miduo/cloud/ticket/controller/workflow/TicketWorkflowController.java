package com.miduo.cloud.ticket.controller.workflow;

import com.miduo.cloud.ticket.application.workflow.TicketWorkflowAppService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.workflow.AvailableActionOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.ReturnInput;
import com.miduo.cloud.ticket.entity.dto.workflow.TransferInput;
import com.miduo.cloud.ticket.entity.dto.workflow.TransitInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 工单工作流操作接口 - 状态流转、转派、退回
 */
@Tag(name = "工单工作流操作", description = "工单状态流转、转派、退回")
@RestController
@RequestMapping("/api/ticket")
public class TicketWorkflowController {

    private final TicketWorkflowAppService ticketWorkflowAppService;

    public TicketWorkflowController(TicketWorkflowAppService ticketWorkflowAppService) {
        this.ticketWorkflowAppService = ticketWorkflowAppService;
    }

    /**
     * 获取可用操作
     * 接口编号：API000014
     * 产品文档功能：4.4 工作流引擎 - 可用操作
     */
    @Operation(summary = "获取工单可用操作", description = "接口编号：API000014")
    @GetMapping("/{id}/available-actions")
    public ApiResult<AvailableActionOutput> getAvailableActions(@PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        AvailableActionOutput result = ticketWorkflowAppService.getAvailableActions(id, operatorId);
        return ApiResult.success(result);
    }

    /**
     * 状态流转
     * 接口编号：API000015
     * 产品文档功能：4.4 工作流引擎 - 状态流转
     */
    @Operation(summary = "执行状态流转", description = "接口编号：API000015")
    @PutMapping("/transit/{id}")
    public ApiResult<Void> transit(@PathVariable Long id, @Valid @RequestBody TransitInput input) {
        Long operatorId = getCurrentUserId();
        ticketWorkflowAppService.transit(id, input, operatorId);
        return ApiResult.success();
    }

    /**
     * 转派
     * 接口编号：API000016
     * 产品文档功能：4.5 分派与路由 - 同角色转派
     */
    @Operation(summary = "转派工单", description = "接口编号：API000016")
    @PutMapping("/transfer/{id}")
    public ApiResult<Void> transfer(@PathVariable Long id, @Valid @RequestBody TransferInput input) {
        Long operatorId = getCurrentUserId();
        ticketWorkflowAppService.transfer(id, input, operatorId);
        return ApiResult.success();
    }

    /**
     * 退回
     * 接口编号：API000017
     * 产品文档功能：4.5 分派与路由 - 退回上一节点
     */
    @Operation(summary = "退回工单", description = "接口编号：API000017")
    @PutMapping("/return/{id}")
    public ApiResult<Void> returnTicket(@PathVariable Long id, @Valid @RequestBody ReturnInput input) {
        Long operatorId = getCurrentUserId();
        ticketWorkflowAppService.returnTicket(id, input, operatorId);
        return ApiResult.success();
    }

    /**
     * 获取当前登录用户ID（待Task003 JWT认证完成后替换）
     */
    private Long getCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        return userId != null ? userId : 1L;
    }
}
