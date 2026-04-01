package com.miduo.cloud.ticket.controller.alert;

import com.miduo.cloud.ticket.application.alert.AlertTicketApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.entity.dto.alert.NightingaleAlertEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 告警Webhook接收控制器（公开端点，Token鉴权）
 */
@Tag(name = "告警Webhook", description = "接收夜莺监控平台告警推送")
@RestController
@RequestMapping("/api/open/alert")
public class AlertWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AlertWebhookController.class);

    private final AlertTicketApplicationService alertTicketService;

    public AlertWebhookController(AlertTicketApplicationService alertTicketService) {
        this.alertTicketService = alertTicketService;
    }

    /**
     * 夜莺告警Webhook回调
     * 接口编号：API000700
     * 产品文档功能：告警监控接入工单系统 - Webhook接收端点
     *
     * 夜莺通过 HTTP POST 将 AlertCurEvent JSON 推送至此端点，
     * 系统解析后根据映射规则自动创建工单。
     */
    @Operation(summary = "夜莺告警Webhook回调", description = "接口编号：API000700")
    @PostMapping("/nightingale/webhook")
    public ApiResult<String> receiveAlert(@RequestParam("token") String token,
                                          @RequestBody NightingaleAlertEvent event) {
        if (!alertTicketService.validateToken(token)) {
            log.warn("告警Webhook Token验证失败");
            return ApiResult.fail(ErrorCode.UNAUTHORIZED.getCode(), "Token无效");
        }

        try {
            alertTicketService.processAlertEvent(event);
            return ApiResult.success("OK");
        } catch (Exception e) {
            log.error("处理告警事件异常: ruleName={}, error={}",
                    event != null ? event.getRuleName() : "null", e.getMessage(), e);
            return ApiResult.success("ACCEPTED");
        }
    }
}
