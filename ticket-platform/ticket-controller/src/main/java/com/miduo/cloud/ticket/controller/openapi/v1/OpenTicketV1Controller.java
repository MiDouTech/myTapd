package com.miduo.cloud.ticket.controller.openapi.v1;

import com.miduo.cloud.ticket.application.dashboard.DashboardApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.TicketSource;
import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardOverviewOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCreateInput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketDetailOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketListOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketPageInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 开放能力V1接口
 */
@Tag(name = "开放API V1", description = "对外工单查询与创建能力")
@RestController
@RequestMapping("/api/v1")
public class OpenTicketV1Controller {

    private final TicketApplicationService ticketService;
    private final DashboardApplicationService dashboardService;

    public OpenTicketV1Controller(TicketApplicationService ticketService,
                                  DashboardApplicationService dashboardService) {
        this.ticketService = ticketService;
        this.dashboardService = dashboardService;
    }

    /**
     * 开放API-创建工单
     * 接口编号：API000413
     * 产品文档功能：4.11 开放能力 - 对外创建工单
     */
    @Operation(summary = "开放API-创建工单", description = "接口编号：API000413")
    @PostMapping("/tickets")
    public ApiResult<Long> createTicket(@Valid @RequestBody TicketCreateInput input) {
        Long ticketId = ticketService.createTicket(toOpenApiCreateInput(input), getCurrentUserId());
        return ApiResult.success(ticketId);
    }

    /**
     * 开放API-工单详情
     * 接口编号：API000414
     * 产品文档功能：4.11 开放能力 - 对外查询工单详情
     */
    @Operation(summary = "开放API-工单详情", description = "接口编号：API000414")
    @GetMapping("/tickets/{id}")
    public ApiResult<TicketDetailOutput> detail(@PathVariable Long id) {
        return ApiResult.success(ticketService.getTicketDetail(id, getCurrentUserId()));
    }

    /**
     * 开放API-工单列表
     * 接口编号：API000415
     * 产品文档功能：4.11 开放能力 - 对外查询工单列表
     */
    @Operation(summary = "开放API-工单列表", description = "接口编号：API000415")
    @GetMapping("/tickets")
    public ApiResult<PageOutput<TicketListOutput>> page(@Valid TicketPageInput input) {
        return ApiResult.success(ticketService.getTicketPage(input, getCurrentUserId()));
    }

    /**
     * 开放API-统计概览
     * 接口编号：API000416
     * 产品文档功能：4.11 开放能力 - 对外概览统计
     */
    @Operation(summary = "开放API-统计概览", description = "接口编号：API000416")
    @GetMapping("/statistics/overview")
    public ApiResult<DashboardOverviewOutput> statisticsOverview() {
        return ApiResult.success(dashboardService.getOverview());
    }

    private TicketCreateInput toOpenApiCreateInput(TicketCreateInput input) {
        TicketCreateInput output = new TicketCreateInput();
        output.setTitle(input.getTitle());
        output.setDescription(input.getDescription());
        output.setCategoryId(input.getCategoryId());
        output.setPriority(input.getPriority());
        output.setExpectedTime(input.getExpectedTime());
        output.setAssigneeId(input.getAssigneeId());
        output.setSource(TicketSource.API.getCode());
        output.setSourceChatId(input.getSourceChatId());
        output.setCustomFields(input.getCustomFields());
        return output;
    }

    private Long getCurrentUserId() {
        return 1L;
    }
}
