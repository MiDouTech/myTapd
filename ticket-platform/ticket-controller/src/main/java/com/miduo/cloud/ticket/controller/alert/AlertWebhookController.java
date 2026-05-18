package com.miduo.cloud.ticket.controller.alert;

import com.miduo.cloud.ticket.application.alert.AlertTicketApplicationService;
import com.miduo.cloud.ticket.application.alert.AlertWebhookPayloadAdapter;
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
    private final AlertWebhookPayloadAdapter alertWebhookPayloadAdapter;

    public AlertWebhookController(AlertTicketApplicationService alertTicketService,
                                  AlertWebhookPayloadAdapter alertWebhookPayloadAdapter) {
        this.alertTicketService = alertTicketService;
        this.alertWebhookPayloadAdapter = alertWebhookPayloadAdapter;
    }

    /**
     * 夜莺告警Webhook回调
     * 接口编号：API000700
     * 产品文档功能：告警监控接入工单系统 - Webhook接收端点
     *
     * 支持两类请求体：（1）夜莺原生 AlertCurEvent JSON；（2）HTTP 通知媒介自定义包装：
     * 根级 {@code notify_users}、{@code rule_name}、{@code severity}、{@code status}、{@code trigger_time}
     * 与内嵌 {@code event}（完整 AlertCurEvent），见 {@link AlertWebhookPayloadAdapter}。
     */
    @Operation(summary = "夜莺告警Webhook回调", description = "接口编号：API000700")
    @PostMapping("/nightingale/webhook")
    public ApiResult<String> receiveAlert(@RequestParam("token") String token,
                                          @RequestBody String rawBody) {
        if (!alertTicketService.validateToken(token)) {
            log.warn("告警Webhook Token验证失败");
            return ApiResult.fail(ErrorCode.UNAUTHORIZED.getCode(), "Token无效");
        }

        NightingaleAlertEvent event = null;
        try {
            event = alertWebhookPayloadAdapter.parse(rawBody);
            alertTicketService.processAlertEvent(event, rawBody);
            return ApiResult.success("OK");
        } catch (Exception e) {
            log.error("处理告警事件异常: ruleName={}, error={}",
                    event != null ? event.getRuleName() : "null", e.getMessage(), e);
            return ApiResult.success("ACCEPTED");
        }
    }
}
