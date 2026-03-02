package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.TicketUrgeApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单催办控制器
 */
@Tag(name = "工单催办", description = "工单催办功能")
@RestController
@RequestMapping("/api/ticket")
public class TicketUrgeController {

    private final TicketUrgeApplicationService ticketUrgeApplicationService;

    public TicketUrgeController(TicketUrgeApplicationService ticketUrgeApplicationService) {
        this.ticketUrgeApplicationService = ticketUrgeApplicationService;
    }

    /**
     * 催办工单
     * 接口编号：API000010
     * 产品文档功能：4.8 通知中心 - 催办功能
     */
    @Operation(summary = "催办工单", description = "接口编号：API000010")
    @PostMapping("/urge/{id}")
    public ApiResult<Void> urgeTicket(@PathVariable("id") Long ticketId) {
        Long currentUserId = getCurrentUserId();
        ticketUrgeApplicationService.urgeByTicketId(ticketId, currentUserId);
        return ApiResult.success();
    }

    /**
     * 获取当前登录用户ID
     * TODO: Task003中集成SecurityContext后替换
     */
    private Long getCurrentUserId() {
        return 1L;
    }
}
