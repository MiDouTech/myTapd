package com.miduo.cloud.ticket.controller.webhook;

import com.miduo.cloud.ticket.application.webhook.WebhookConfigApplicationService;
import com.miduo.cloud.ticket.application.webhook.WebhookDispatchLogApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookConfigCreateInput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookConfigOutput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookConfigPageInput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookConfigUpdateInput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookDispatchLogOutput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookDispatchLogPageInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Webhook配置控制器
 */
@Tag(name = "Webhook配置", description = "Webhook地址与事件配置管理")
@RestController
@RequestMapping("/api/webhook/config")
public class WebhookConfigController {

    private final WebhookConfigApplicationService webhookConfigService;
    private final WebhookDispatchLogApplicationService webhookDispatchLogService;

    public WebhookConfigController(WebhookConfigApplicationService webhookConfigService,
                                   WebhookDispatchLogApplicationService webhookDispatchLogService) {
        this.webhookConfigService = webhookConfigService;
        this.webhookDispatchLogService = webhookDispatchLogService;
    }

    /**
     * 分页查询Webhook配置
     * 接口编号：API000417
     * 产品文档功能：4.11 开放能力 - Webhook配置管理
     */
    @Operation(summary = "分页查询Webhook配置", description = "接口编号：API000417")
    @GetMapping("/page")
    public ApiResult<PageOutput<WebhookConfigOutput>> page(@Valid WebhookConfigPageInput input) {
        return ApiResult.success(webhookConfigService.page(input));
    }

    /**
     * 查询Webhook配置详情
     * 接口编号：API000418
     * 产品文档功能：4.11 开放能力 - Webhook配置详情
     */
    @Operation(summary = "查询Webhook配置详情", description = "接口编号：API000418")
    @GetMapping("/detail/{id}")
    public ApiResult<WebhookConfigOutput> detail(@PathVariable Long id) {
        return ApiResult.success(webhookConfigService.detail(id));
    }

    /**
     * 创建Webhook配置
     * 接口编号：API000419
     * 产品文档功能：4.11 开放能力 - 新增Webhook配置
     */
    @Operation(summary = "创建Webhook配置", description = "接口编号：API000419")
    @PostMapping("/create")
    public ApiResult<Long> create(@Valid @RequestBody WebhookConfigCreateInput input) {
        return ApiResult.success(webhookConfigService.create(input));
    }

    /**
     * 更新Webhook配置
     * 接口编号：API000420
     * 产品文档功能：4.11 开放能力 - 更新Webhook配置
     */
    @Operation(summary = "更新Webhook配置", description = "接口编号：API000420")
    @PutMapping("/update/{id}")
    public ApiResult<Void> update(@PathVariable Long id,
                                  @Valid @RequestBody WebhookConfigUpdateInput input) {
        webhookConfigService.update(id, input);
        return ApiResult.success();
    }

    /**
     * 删除Webhook配置
     * 接口编号：API000421
     * 产品文档功能：4.11 开放能力 - 删除Webhook配置
     */
    @Operation(summary = "删除Webhook配置", description = "接口编号：API000421")
    @DeleteMapping("/delete/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        webhookConfigService.delete(id);
        return ApiResult.success();
    }

    /**
     * 分页查询Webhook推送日志
     * 接口编号：API000431
     * 产品文档功能：4.11 开放能力 - Webhook推送日志排障
     */
    @Operation(summary = "分页查询Webhook推送日志", description = "接口编号：API000431")
    @GetMapping("/dispatch-log/page")
    public ApiResult<PageOutput<WebhookDispatchLogOutput>> dispatchLogPage(@Valid WebhookDispatchLogPageInput input) {
        return ApiResult.success(webhookDispatchLogService.page(input));
    }
}
