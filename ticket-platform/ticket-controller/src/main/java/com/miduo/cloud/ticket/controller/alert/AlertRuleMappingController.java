package com.miduo.cloud.ticket.controller.alert;

import com.miduo.cloud.ticket.application.alert.AlertTicketApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.alert.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 告警规则映射配置控制器（需登录鉴权）
 */
@Tag(name = "告警规则映射配置", description = "管理夜莺告警规则与工单系统的映射关系")
@RestController
@RequestMapping("/api/alert-mapping")
public class AlertRuleMappingController {

    private final AlertTicketApplicationService alertTicketService;

    public AlertRuleMappingController(AlertTicketApplicationService alertTicketService) {
        this.alertTicketService = alertTicketService;
    }

    /**
     * 分页查询告警规则映射
     * 接口编号：API000701
     * 产品文档功能：告警接入 - 映射配置列表
     */
    @Operation(summary = "分页查询告警规则映射", description = "接口编号：API000701")
    @GetMapping("/page")
    public ApiResult<PageOutput<AlertRuleMappingOutput>> page(@Valid AlertRuleMappingPageInput input) {
        return ApiResult.success(alertTicketService.getMappingPage(input));
    }

    /**
     * 创建告警规则映射
     * 接口编号：API000702
     * 产品文档功能：告警接入 - 新增映射配置
     */
    @OperationLog(moduleName = "告警接入", operationItem = "创建告警规则映射")
    @Operation(summary = "创建告警规则映射", description = "接口编号：API000702")
    @PostMapping("/create")
    public ApiResult<Long> create(@Valid @RequestBody AlertRuleMappingCreateInput input) {
        return ApiResult.success(alertTicketService.createMapping(input));
    }

    /**
     * 更新告警规则映射
     * 接口编号：API000703
     * 产品文档功能：告警接入 - 编辑映射配置
     */
    @OperationLog(moduleName = "告警接入", operationItem = "更新告警规则映射")
    @Operation(summary = "更新告警规则映射", description = "接口编号：API000703")
    @PutMapping("/update")
    public ApiResult<Void> update(@Valid @RequestBody AlertRuleMappingUpdateInput input) {
        alertTicketService.updateMapping(input);
        return ApiResult.success(null);
    }

    /**
     * 删除告警规则映射
     * 接口编号：API000704
     * 产品文档功能：告警接入 - 删除映射配置
     */
    @OperationLog(moduleName = "告警接入", operationItem = "删除告警规则映射")
    @Operation(summary = "删除告警规则映射", description = "接口编号：API000704")
    @DeleteMapping("/delete/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        alertTicketService.deleteMapping(id);
        return ApiResult.success(null);
    }

    /**
     * 获取告警规则映射详情
     * 接口编号：API000705
     * 产品文档功能：告警接入 - 映射配置详情
     */
    @Operation(summary = "获取告警规则映射详情", description = "接口编号：API000705")
    @GetMapping("/detail/{id}")
    public ApiResult<AlertRuleMappingOutput> detail(@PathVariable Long id) {
        return ApiResult.success(alertTicketService.getMappingDetail(id));
    }

    /**
     * 分页查询告警事件日志
     * 接口编号：API000706
     * 产品文档功能：告警接入 - 告警事件日志
     */
    @Operation(summary = "分页查询告警事件日志", description = "接口编号：API000706")
    @GetMapping("/event-log/page")
    public ApiResult<PageOutput<AlertEventLogOutput>> eventLogPage(@Valid AlertEventLogPageInput input) {
        return ApiResult.success(alertTicketService.getEventLogPage(input));
    }

    /**
     * 获取告警接入Token
     * 接口编号：API000707
     * 产品文档功能：告警接入 - 获取Webhook地址与Token
     */
    @Operation(summary = "获取告警接入Token", description = "接口编号：API000707")
    @GetMapping("/token")
    public ApiResult<AlertTokenOutput> getToken(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        return ApiResult.success(alertTicketService.getToken(baseUrl));
    }

    /**
     * 重置告警接入Token
     * 接口编号：API000708
     * 产品文档功能：告警接入 - 重新生成Token
     */
    @OperationLog(moduleName = "告警接入", operationItem = "重置告警接入Token")
    @Operation(summary = "重置告警接入Token", description = "接口编号：API000708")
    @PostMapping("/token/reset")
    public ApiResult<AlertTokenOutput> resetToken(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        return ApiResult.success(alertTicketService.resetToken(baseUrl));
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        if (("http".equals(scheme) && serverPort == 80) ||
                ("https".equals(scheme) && serverPort == 443)) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + serverPort;
    }
}
