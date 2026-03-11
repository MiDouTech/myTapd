package com.miduo.cloud.ticket.controller.openapi.v1;

import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketPublicDetailOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单公开访问接口（无需登录，支持外网直接访问）
 */
@Tag(name = "工单公开接口", description = "无需登录，支持外网直接查看工单详情")
@RestController
@RequestMapping("/open/ticket")
public class OpenTicketPublicController {

    private final TicketApplicationService ticketApplicationService;

    public OpenTicketPublicController(TicketApplicationService ticketApplicationService) {
        this.ticketApplicationService = ticketApplicationService;
    }

    /**
     * 按工单编号查询公开详情（无需登录）
     * 接口编号：API000417
     * 产品文档功能：4.12 工单公开链接 - 外网无需登录查看工单详情
     */
    @Operation(summary = "工单公开详情（无需登录）", description = "接口编号：API000417，支持手机端访问")
    @GetMapping("/{ticketNo}")
    public ApiResult<TicketPublicDetailOutput> getPublicDetail(@PathVariable String ticketNo) {
        TicketPublicDetailOutput output = ticketApplicationService.getPublicTicketDetail(ticketNo);
        return ApiResult.success(output);
    }
}
