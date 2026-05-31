package com.miduo.cloud.ticket.controller.weeklyreport;

import com.miduo.cloud.ticket.application.weeklyreport.WeeklyInvalidReportApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportConfigOutput;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportConfigUpdateInput;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 无效反馈周报管理控制器
 */
@Tag(name = "无效反馈周报管理", description = "无效反馈周报预览、推送、配置管理")
@RestController
@RequestMapping("/api/weekly-invalid-report")
public class WeeklyInvalidReportController {

    private final WeeklyInvalidReportApplicationService weeklyInvalidReportService;

    public WeeklyInvalidReportController(WeeklyInvalidReportApplicationService weeklyInvalidReportService) {
        this.weeklyInvalidReportService = weeklyInvalidReportService;
    }

    /**
     * 预览无效反馈周报（不推送，仅返回数据和Markdown）
     * 接口编号：API000517
     * 产品文档功能：无效反馈周报推送 - 预览周报内容
     */
    @Operation(summary = "预览无效反馈周报", description = "接口编号：API000517")
    @GetMapping("/preview")
    public ApiResult<WeeklyInvalidReportOutput> preview() {
        return ApiResult.success(weeklyInvalidReportService.generateWeeklyInvalidReport());
    }

    /**
     * 手动推送无效反馈周报
     * 接口编号：API000518
     * 产品文档功能：无效反馈周报推送 - 手动推送周报
     */
    @Operation(summary = "手动推送无效反馈周报", description = "接口编号：API000518")
    @PostMapping("/push")
    public ApiResult<Void> push() {
        weeklyInvalidReportService.pushWeeklyInvalidReportManually();
        return ApiResult.success();
    }

    /**
     * 查询无效反馈周报配置
     * 接口编号：API000519
     * 产品文档功能：无效反馈周报推送 - 查询配置
     */
    @Operation(summary = "查询无效反馈周报配置", description = "接口编号：API000519")
    @GetMapping("/config")
    public ApiResult<WeeklyInvalidReportConfigOutput> getConfig() {
        return ApiResult.success(weeklyInvalidReportService.getConfig());
    }

    /**
     * 更新无效反馈周报配置
     * 接口编号：API000520
     * 产品文档功能：无效反馈周报推送 - 更新配置
     */
    @Operation(summary = "更新无效反馈周报配置", description = "接口编号：API000520")
    @PutMapping("/config")
    public ApiResult<Void> updateConfig(@Valid @RequestBody WeeklyInvalidReportConfigUpdateInput input) {
        weeklyInvalidReportService.updateConfig(input);
        return ApiResult.success();
    }
}
