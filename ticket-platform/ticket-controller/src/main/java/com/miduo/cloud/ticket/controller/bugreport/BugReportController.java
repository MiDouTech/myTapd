package com.miduo.cloud.ticket.controller.bugreport;

import com.miduo.cloud.ticket.application.bugreport.BugReportApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.entity.dto.bugreport.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Bug简报管理控制器
 */
@RestController
@RequestMapping("/api/bug-report")
@Tag(name = "Bug简报管理", description = "Bug简报CRUD、审核、统计接口")
public class BugReportController {

    private final BugReportApplicationService bugReportApplicationService;

    public BugReportController(BugReportApplicationService bugReportApplicationService) {
        this.bugReportApplicationService = bugReportApplicationService;
    }

    /**
     * 分页查询Bug简报
     * 接口编号：API000020
     * 产品文档功能：4.12 Bug简报管理 - 简报列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询Bug简报", description = "接口编号：API000020")
    public ApiResult<PageOutput<BugReportPageOutput>> page(@Valid BugReportPageInput input) {
        return ApiResult.success(bugReportApplicationService.page(input));
    }

    /**
     * 获取Bug简报详情
     * 接口编号：API000021
     * 产品文档功能：4.12 Bug简报管理 - 简报详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取Bug简报详情", description = "接口编号：API000021")
    public ApiResult<BugReportDetailOutput> detail(@PathVariable("id") Long id) {
        return ApiResult.success(bugReportApplicationService.detail(id));
    }

    /**
     * 创建Bug简报
     * 接口编号：API000022
     * 产品文档功能：4.12 Bug简报管理 - 简报创建
     */
    @PostMapping("/create")
    @Operation(summary = "创建Bug简报", description = "接口编号：API000022")
    public ApiResult<Long> create(@Valid @RequestBody BugReportCreateInput input) {
        return ApiResult.success(bugReportApplicationService.create(input, getCurrentUserId()));
    }

    /**
     * 更新Bug简报
     * 接口编号：API000023
     * 产品文档功能：4.12 Bug简报管理 - 简报编辑
     */
    @PutMapping("/update/{id}")
    @Operation(summary = "更新Bug简报", description = "接口编号：API000023")
    public ApiResult<Void> update(@PathVariable("id") Long id, @RequestBody BugReportUpdateInput input) {
        bugReportApplicationService.update(id, input, getCurrentUserId());
        return ApiResult.success();
    }

    /**
     * 提交审核
     * 接口编号：API000024
     * 产品文档功能：4.12 Bug简报管理 - 提交审核
     */
    @PutMapping("/submit/{id}")
    @Operation(summary = "提交Bug简报审核", description = "接口编号：API000024")
    public ApiResult<Void> submit(@PathVariable("id") Long id,
                                  @RequestBody(required = false) BugReportSubmitInput input) {
        bugReportApplicationService.submit(id, input, getCurrentUserId());
        return ApiResult.success();
    }

    /**
     * 审核通过
     * 接口编号：API000025
     * 产品文档功能：4.12 Bug简报管理 - 审核通过归档
     */
    @PutMapping("/approve/{id}")
    @Operation(summary = "审核通过", description = "接口编号：API000025")
    public ApiResult<Void> approve(@PathVariable("id") Long id, @Valid @RequestBody BugReportReviewInput input) {
        bugReportApplicationService.approve(id, input, getCurrentUserId());
        return ApiResult.success();
    }

    /**
     * 审核驳回
     * 接口编号：API000026
     * 产品文档功能：4.12 Bug简报管理 - 审核不通过
     */
    @PutMapping("/reject/{id}")
    @Operation(summary = "审核驳回", description = "接口编号：API000026")
    public ApiResult<Void> reject(@PathVariable("id") Long id, @Valid @RequestBody BugReportReviewInput input) {
        bugReportApplicationService.reject(id, input, getCurrentUserId());
        return ApiResult.success();
    }

    /**
     * 作废简报
     * 接口编号：API000027
     * 产品文档功能：4.12 Bug简报管理 - 作废
     */
    @PutMapping("/void/{id}")
    @Operation(summary = "作废简报", description = "接口编号：API000027")
    public ApiResult<Void> voidReport(@PathVariable("id") Long id) {
        bugReportApplicationService.voidReport(id, getCurrentUserId());
        return ApiResult.success();
    }

    /**
     * Bug简报统计看板
     * 接口编号：API000028
     * 产品文档功能：4.12 Bug简报管理 - 统计看板
     */
    @GetMapping("/statistics")
    @Operation(summary = "Bug简报统计看板", description = "接口编号：API000028")
    public ApiResult<BugReportStatisticsOutput> statistics(BugReportStatisticsInput input) {
        return ApiResult.success(bugReportApplicationService.statistics(input));
    }

    private Long getCurrentUserId() {
        // TODO: Task003集成SecurityContext后替换
        return 1L;
    }
}
