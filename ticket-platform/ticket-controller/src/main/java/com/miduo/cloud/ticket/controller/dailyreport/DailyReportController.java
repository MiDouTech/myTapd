package com.miduo.cloud.ticket.controller.dailyreport;

import com.miduo.cloud.ticket.application.dailyreport.DailyReportApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportConfigOutput;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportConfigUpdateInput;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 日报管理控制器
 */
@Tag(name = "日报管理", description = "日报预览、手动推送、配置管理")
@RestController
@RequestMapping("/api/daily-report")
public class DailyReportController {

    private final DailyReportApplicationService dailyReportService;

    public DailyReportController(DailyReportApplicationService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    /**
     * 预览日报（不推送，仅返回数据和 Markdown）
     * 接口编号：API000501
     * 产品文档功能：日报自动推送 - 预览日报内容
     */
    @Operation(summary = "预览日报", description = "接口编号：API000501")
    @GetMapping("/preview")
    public ApiResult<DailyReportOutput> preview() {
        return ApiResult.success(dailyReportService.generateDailyReport());
    }

    /**
     * 手动触发日报推送
     * 接口编号：API000502
     * 产品文档功能：日报自动推送 - 手动推送日报
     */
    @Operation(summary = "手动推送日报", description = "接口编号：API000502")
    @PostMapping("/push")
    public ApiResult<Void> push() {
        dailyReportService.pushDailyReport();
        return ApiResult.success();
    }

    /**
     * 查询日报配置
     * 接口编号：API000503
     * 产品文档功能：日报自动推送 - 查询配置
     */
    @Operation(summary = "查询日报配置", description = "接口编号：API000503")
    @GetMapping("/config")
    public ApiResult<DailyReportConfigOutput> getConfig() {
        return ApiResult.success(dailyReportService.getConfig());
    }

    /**
     * 更新日报配置
     * 接口编号：API000504
     * 产品文档功能：日报自动推送 - 更新配置
     */
    @Operation(summary = "更新日报配置", description = "接口编号：API000504")
    @PutMapping("/config")
    public ApiResult<Void> updateConfig(@Valid @RequestBody DailyReportConfigUpdateInput input) {
        dailyReportService.updateConfig(input);
        return ApiResult.success();
    }
}
