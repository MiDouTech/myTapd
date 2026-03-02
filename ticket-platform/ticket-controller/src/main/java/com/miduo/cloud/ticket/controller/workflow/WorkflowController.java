package com.miduo.cloud.ticket.controller.workflow;

import com.miduo.cloud.ticket.application.workflow.WorkflowAppService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.workflow.WorkflowDetailOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.WorkflowListOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作流管理接口
 */
@Tag(name = "工作流管理", description = "工作流定义查询")
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final WorkflowAppService workflowAppService;

    public WorkflowController(WorkflowAppService workflowAppService) {
        this.workflowAppService = workflowAppService;
    }

    /**
     * 工作流列表
     * 接口编号：API000012
     * 产品文档功能：4.4 工作流引擎 - 工作流列表
     */
    @Operation(summary = "工作流列表", description = "接口编号：API000012")
    @GetMapping("/list")
    public ApiResult<List<WorkflowListOutput>> listWorkflows() {
        List<WorkflowListOutput> result = workflowAppService.listWorkflows();
        return ApiResult.success(result);
    }

    /**
     * 工作流详情
     * 接口编号：API000013
     * 产品文档功能：4.4 工作流引擎 - 工作流详情
     */
    @Operation(summary = "工作流详情", description = "接口编号：API000013")
    @GetMapping("/detail/{id}")
    public ApiResult<WorkflowDetailOutput> getWorkflowDetail(@PathVariable Long id) {
        WorkflowDetailOutput result = workflowAppService.getWorkflowDetail(id);
        return ApiResult.success(result);
    }
}
