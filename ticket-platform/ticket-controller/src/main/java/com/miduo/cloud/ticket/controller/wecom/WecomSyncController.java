package com.miduo.cloud.ticket.controller.wecom;

import com.miduo.cloud.ticket.application.wecom.WecomSyncApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.wecom.SyncLogPageInput;
import com.miduo.cloud.ticket.entity.dto.wecom.SyncManualOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.SyncStatusOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 企业微信组织同步接口
 */
@Tag(name = "企业微信同步", description = "组织架构手动同步与状态查询")
@RestController
@RequestMapping("/api/v1/sync")
public class WecomSyncController {

    private final WecomSyncApplicationService wecomSyncApplicationService;

    public WecomSyncController(WecomSyncApplicationService wecomSyncApplicationService) {
        this.wecomSyncApplicationService = wecomSyncApplicationService;
    }

    /**
     * 手动触发同步
     * 接口编号：API000425
     * 产品文档功能：SSO一期-手动同步
     */
    @OperationLog(moduleName = "企业微信同步", operationItem = "手动触发组织架构同步", recordParams = false)
    @Operation(summary = "手动触发企业微信同步", description = "接口编号：API000425")
    @PostMapping("/manual")
    public ApiResult<SyncManualOutput> manualSync() {
        Long userId = SecurityUtil.getCurrentUserId();
        String triggerBy = userId == null ? "system" : String.valueOf(userId);
        return ApiResult.success(wecomSyncApplicationService.manualSync(triggerBy));
    }

    /**
     * 查询最近同步状态
     * 接口编号：API000426
     * 产品文档功能：SSO一期-同步状态查询
     */
    @Operation(summary = "查询最近同步状态", description = "接口编号：API000426")
    @GetMapping("/status")
    public ApiResult<SyncStatusOutput> latestStatus() {
        return ApiResult.success(wecomSyncApplicationService.latestStatus());
    }

    /**
     * 分页查询同步日志
     * 接口编号：API000427
     * 产品文档功能：SSO一期-同步日志查询
     */
    @Operation(summary = "分页查询同步日志", description = "接口编号：API000427")
    @GetMapping("/log/page")
    public ApiResult<PageOutput<SyncStatusOutput>> pageSyncLogs(@Valid SyncLogPageInput input) {
        return ApiResult.success(wecomSyncApplicationService.pageLogs(input));
    }
}
