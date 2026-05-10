package com.miduo.cloud.ticket.controller.openapi.v1;

import com.miduo.cloud.ticket.application.openapi.OpenTicketBugReportApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.openapi.OpenTicketBugReportOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 外部系统按工单编号查询 Bug 简报接口
 */
@Tag(name = "开放API-Bug简报", description = "外部系统按工单编号查询最新归档的Bug简报")
@RestController
@RequestMapping("/api/open/v1/bug-report")
public class OpenTicketBugReportController {

    private final OpenTicketBugReportApplicationService openTicketBugReportApplicationService;

    public OpenTicketBugReportController(OpenTicketBugReportApplicationService openTicketBugReportApplicationService) {
        this.openTicketBugReportApplicationService = openTicketBugReportApplicationService;
    }

    /**
     * 根据工单编号获取 Bug 简报
     * 接口编号：API000514
     * 产品文档功能：开放接口-按工单编号获取Bug简报
     */
    @OperationLog(moduleName = "开放API", operationItem = "按工单编号查询Bug简报")
    @Operation(summary = "按工单编号查询Bug简报", description = "接口编号：API000514")
    @GetMapping("/detail/{ticketNo}")
    public ApiResult<OpenTicketBugReportOutput> detail(@PathVariable String ticketNo) {
        return ApiResult.success(openTicketBugReportApplicationService.detailByTicketNo(ticketNo));
    }
}
