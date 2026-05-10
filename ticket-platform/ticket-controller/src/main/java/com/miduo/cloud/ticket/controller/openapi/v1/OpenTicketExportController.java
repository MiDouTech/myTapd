package com.miduo.cloud.ticket.controller.openapi.v1;

import com.miduo.cloud.ticket.application.openapi.OpenTicketExportApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.openapi.OpenTicketExportOutput;
import com.miduo.cloud.ticket.entity.dto.openapi.OpenTicketExportPageInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 外部合作系统工单开放查询接口
 */
@Tag(name = "开放API外部拉取", description = "外部系统按时间范围拉取工单全量数据")
@RestController
@RequestMapping("/api/open/v1/ticket-export")
public class OpenTicketExportController {

    private final OpenTicketExportApplicationService openTicketExportApplicationService;

    public OpenTicketExportController(OpenTicketExportApplicationService openTicketExportApplicationService) {
        this.openTicketExportApplicationService = openTicketExportApplicationService;
    }

    /**
     * 外部系统按时间范围分页拉取工单全量数据
     * 接口编号：API000513
     * 产品文档功能：开放接口-按时间范围拉取工单全量数据
     */
    @OperationLog(moduleName = "开放API", operationItem = "外部系统分页拉取工单全量数据")
    @Operation(summary = "外部系统分页拉取工单全量数据", description = "接口编号：API000513")
    @GetMapping("/page")
    public ApiResult<PageOutput<OpenTicketExportOutput>> page(@Valid OpenTicketExportPageInput input) {
        return ApiResult.success(openTicketExportApplicationService.pageExport(input));
    }
}
