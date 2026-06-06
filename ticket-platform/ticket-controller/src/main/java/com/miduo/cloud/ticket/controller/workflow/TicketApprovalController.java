package com.miduo.cloud.ticket.controller.workflow;

import com.miduo.cloud.ticket.application.workflow.TicketApprovalAppService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.workflow.ApprovalActionInput;
import com.miduo.cloud.ticket.entity.dto.workflow.ApprovalPendingListOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.ApprovalTaskOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 工单审批引擎接口
 * 嵌入式审批任务层，对标米多星球 WorkflowFlowInstanceController 核心能力
 */
@Tag(name = "工单审批", description = "审批任务查询与审批操作")
@RestController
@RequestMapping("/api/ticket/approval")
public class TicketApprovalController {

    private final TicketApprovalAppService ticketApprovalAppService;

    public TicketApprovalController(TicketApprovalAppService ticketApprovalAppService) {
        this.ticketApprovalAppService = ticketApprovalAppService;
    }

    /**
     * 查询工单审批任务详情（含审批时间轴）
     * 接口编号：API000517
     * 产品文档功能：审批工单详情 - 审批流程标签页
     */
    @Operation(summary = "查询工单审批任务详情", description = "接口编号：API000517")
    @GetMapping("/{ticketId}/tasks")
    public ApiResult<ApprovalTaskOutput> getApprovalTasks(@PathVariable Long ticketId) {
        Long currentUserId = getCurrentUserId();
        ApprovalTaskOutput result = ticketApprovalAppService.getApprovalTasks(ticketId, currentUserId);
        return ApiResult.success(result);
    }

    /**
     * 查询当前用户待审批任务数量（角标）
     * 接口编号：API000518
     * 产品文档功能：导航栏待审批角标
     */
    @Operation(summary = "待审批任务数量", description = "接口编号：API000518")
    @GetMapping("/my-pending-count")
    public ApiResult<Long> getMyPendingCount() {
        Long currentUserId = getCurrentUserId();
        long count = ticketApprovalAppService.getPendingCount(currentUserId);
        return ApiResult.success(count);
    }

    /**
     * 查询当前用户待审批任务列表
     * 接口编号：API000519
     * 产品文档功能：待我审批列表页
     */
    @Operation(summary = "待审批任务列表", description = "接口编号：API000519")
    @GetMapping("/my-pending")
    public ApiResult<ApprovalPendingListOutput> getMyPendingList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long currentUserId = getCurrentUserId();
        ApprovalPendingListOutput result = ticketApprovalAppService.getPendingList(currentUserId, pageNum, pageSize);
        return ApiResult.success(result);
    }

    /**
     * 执行审批操作（同意 / 驳回 / 转交）
     * 接口编号：API000520
     * 产品文档功能：审批工单 - 审批操作弹窗
     */
    @OperationLog(moduleName = "工单审批", operationItem = "执行审批操作")
    @Operation(summary = "执行审批操作", description = "接口编号：API000520")
    @PostMapping("/action")
    public ApiResult<Void> performApprove(@Valid @RequestBody ApprovalActionInput input) {
        Long currentUserId = getCurrentUserId();
        ticketApprovalAppService.performApprove(input, currentUserId);
        return ApiResult.success();
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
