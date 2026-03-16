package com.miduo.cloud.ticket.controller.operationlog;

import com.miduo.cloud.ticket.application.operationlog.OperationLogApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.entity.dto.operationlog.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 操作日志（工单日志）控制器
 * 接口编号段：API000600–API000699
 * PRD：miduo-md/business/工单日志模块PRD.md
 */
@Tag(name = "操作日志", description = "工单日志管理，包含日志列表查询、详情查看、统计概览")
@RestController
@RequestMapping("/api/operation-log")
public class OperationLogController {

    private final OperationLogApplicationService operationLogService;

    public OperationLogController(OperationLogApplicationService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询操作日志
     * 接口编号：API000600
     * 产品文档功能：PRD §3.1 日志列表多条件查询
     */
    @Operation(summary = "分页查询操作日志", description = "接口编号：API000600")
    @GetMapping("/page")
    public ApiResult<PageOutput<OperationLogListOutput>> page(@Valid OperationLogPageInput input) {
        return ApiResult.success(operationLogService.page(input));
    }

    /**
     * 获取操作日志详情
     * 接口编号：API000601
     * 产品文档功能：PRD §3.2 日志详情抽屉
     */
    @Operation(summary = "获取操作日志详情", description = "接口编号：API000601")
    @GetMapping("/detail/{id}")
    public ApiResult<OperationLogDetailOutput> detail(
            @Parameter(description = "日志ID") @PathVariable Long id) {
        OperationLogDetailOutput detail = operationLogService.detail(id);
        if (detail == null) {
            return ApiResult.fail(404, "日志记录不存在");
        }
        return ApiResult.success(detail);
    }

    /**
     * 获取日志统计概览（今日数据）
     * 接口编号：API000602
     * 产品文档功能：PRD §3.3 今日日志统计卡片
     */
    @Operation(summary = "获取日志统计概览", description = "接口编号：API000602")
    @GetMapping("/statistics")
    public ApiResult<OperationLogStatisticsOutput> statistics() {
        return ApiResult.success(operationLogService.statistics());
    }

    /**
     * 导出操作日志（预留接口）
     * 接口编号：API000603
     * 产品文档功能：PRD §3.4 日志导出Excel（v1.0 预留，暂不实现）
     */
    @Operation(summary = "导出操作日志（预留）", description = "接口编号：API000603，当前版本预留，暂不实现导出逻辑")
    @GetMapping("/export")
    public ApiResult<Void> export(OperationLogPageInput input) {
        return ApiResult.fail(501, "导出功能暂未开放，请等待后续版本");
    }

    /**
     * 获取操作模块枚举列表
     * 接口编号：API000604
     * 产品文档功能：PRD §3.1 操作模块下拉选项
     */
    @Operation(summary = "获取操作模块枚举列表", description = "接口编号：API000604")
    @GetMapping("/module/list")
    public ApiResult<List<String>> listModules() {
        return ApiResult.success(operationLogService.listModuleNames());
    }

    /**
     * 获取所属应用枚举列表
     * 接口编号：API000605
     * 产品文档功能：PRD §3.1 所属应用下拉选项
     */
    @Operation(summary = "获取所属应用枚举列表", description = "接口编号：API000605")
    @GetMapping("/app/list")
    public ApiResult<List<AppCodeOutput>> listApps() {
        return ApiResult.success(operationLogService.listAppCodes());
    }
}
