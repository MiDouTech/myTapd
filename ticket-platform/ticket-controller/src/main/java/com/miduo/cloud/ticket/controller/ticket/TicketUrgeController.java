package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.TicketUrgeApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketUrgeInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * 接口编号：API000029
     * 产品文档功能：工单详情 - 中间态催办（默认通知关联处理人，可追加通知人）
     */
    @OperationLog(moduleName = "工单管理", operationItem = "催办工单", recordParams = false)
    @Operation(summary = "催办工单", description = "接口编号：API000029")
    @PostMapping("/urge/{id}")
    public ApiResult<Void> urgeTicket(@PathVariable("id") Long ticketId,
                                      @RequestBody(required = false) TicketUrgeInput input) {
        Long currentUserId = getCurrentUserId();
        ticketUrgeApplicationService.urgeByTicketId(ticketId, currentUserId, input);
        return ApiResult.success();
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
