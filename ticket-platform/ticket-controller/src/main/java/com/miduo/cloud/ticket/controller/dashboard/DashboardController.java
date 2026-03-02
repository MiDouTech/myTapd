package com.miduo.cloud.ticket.controller.dashboard;

import com.miduo.cloud.ticket.application.dashboard.DashboardApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.dashboard.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据看板控制器
 */
@Tag(name = "数据看板", description = "工单概览、趋势、分类、效率、SLA、工作量")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardApplicationService dashboardService;

    public DashboardController(DashboardApplicationService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 工单概览仪表盘
     * 接口编号：API000405
     * 产品文档功能：4.9 数据看板与报表 - 工单概览
     */
    @Operation(summary = "工单概览仪表盘", description = "接口编号：API000405")
    @GetMapping("/overview")
    public ApiResult<DashboardOverviewOutput> overview() {
        return ApiResult.success(dashboardService.getOverview());
    }

    /**
     * 工单趋势统计
     * 接口编号：API000406
     * 产品文档功能：4.9 数据看板与报表 - 工单趋势（新建/关闭/积压）
     */
    @Operation(summary = "工单趋势统计", description = "接口编号：API000406")
    @GetMapping("/trend")
    public ApiResult<List<DashboardTrendPointOutput>> trend(@RequestParam(required = false) Integer days) {
        return ApiResult.success(dashboardService.getTrend(days));
    }

    /**
     * 分类分布统计
     * 接口编号：API000407
     * 产品文档功能：4.9 数据看板与报表 - 分类分布
     */
    @Operation(summary = "分类分布统计", description = "接口编号：API000407")
    @GetMapping("/category-distribution")
    public ApiResult<List<DashboardCategoryDistributionOutput>> categoryDistribution() {
        return ApiResult.success(dashboardService.getCategoryDistribution());
    }

    /**
     * 处理效率统计
     * 接口编号：API000408
     * 产品文档功能：4.9 数据看板与报表 - 处理效率
     */
    @Operation(summary = "处理效率统计", description = "接口编号：API000408")
    @GetMapping("/efficiency")
    public ApiResult<DashboardEfficiencyOutput> efficiency() {
        return ApiResult.success(dashboardService.getEfficiency());
    }

    /**
     * SLA达成统计
     * 接口编号：API000409
     * 产品文档功能：4.9 数据看板与报表 - SLA达成率
     */
    @Operation(summary = "SLA达成统计", description = "接口编号：API000409")
    @GetMapping("/sla-achievement")
    public ApiResult<DashboardSlaAchievementOutput> slaAchievement() {
        return ApiResult.success(dashboardService.getSlaAchievement());
    }

    /**
     * 人员工作量TOP
     * 接口编号：API000410
     * 产品文档功能：4.9 数据看板与报表 - 人员工作量TOP10
     */
    @Operation(summary = "人员工作量TOP", description = "接口编号：API000410")
    @GetMapping("/workload")
    public ApiResult<List<DashboardWorkloadOutput>> workload(@RequestParam(required = false) Integer limit) {
        return ApiResult.success(dashboardService.getWorkloadTop(limit));
    }
}
