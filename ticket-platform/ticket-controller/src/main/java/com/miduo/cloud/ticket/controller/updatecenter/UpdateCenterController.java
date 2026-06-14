package com.miduo.cloud.ticket.controller.updatecenter;

import com.miduo.cloud.ticket.application.updatecenter.UpdateCenterApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.updatecenter.UpdateCenterOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 更新中心控制器
 */
@Tag(name = "更新中心", description = "仓库更新碎片、版本日志和Git提交查询")
@RestController
@RequestMapping("/api/update-center")
public class UpdateCenterController {

    private final UpdateCenterApplicationService updateCenterApplicationService;

    public UpdateCenterController(UpdateCenterApplicationService updateCenterApplicationService) {
        this.updateCenterApplicationService = updateCenterApplicationService;
    }

    /**
     * 查询待发布更新
     * 接口编号：API000521
     * 产品文档功能：管理 / 更新中心 - 待发布更新列表
     */
    @Operation(summary = "查询待发布更新", description = "接口编号：API000521")
    @GetMapping("/current-week")
    public ApiResult<UpdateCenterOutput.CurrentWeekOutput> getCurrentWeek(
            @RequestParam(value = "daysLimit", required = false) Integer daysLimit,
            @RequestParam(value = "daysOffset", required = false) Integer daysOffset,
            @RequestParam(value = "force", required = false) Boolean force) {
        return ApiResult.success(updateCenterApplicationService.getCurrentWeek(daysLimit, daysOffset, force));
    }

    /**
     * 查询已发布更新
     * 接口编号：API000522
     * 产品文档功能：管理 / 更新中心 - 已发布版本列表
     */
    @Operation(summary = "查询已发布更新", description = "接口编号：API000522")
    @GetMapping("/releases")
    public ApiResult<UpdateCenterOutput.ReleasesOutput> getReleases(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "summary", required = false) Boolean summary,
            @RequestParam(value = "force", required = false) Boolean force) {
        return ApiResult.success(updateCenterApplicationService.getReleases(limit, summary, force));
    }

    /**
     * 查询指定版本更新详情
     * 接口编号：API000523
     * 产品文档功能：管理 / 更新中心 - 版本更新详情
     */
    @Operation(summary = "查询指定版本更新详情", description = "接口编号：API000523")
    @GetMapping("/releases/detail/{version}")
    public ApiResult<UpdateCenterOutput.ChangelogReleaseOutput> getReleaseDetail(
            @PathVariable("version") String version,
            @RequestParam(value = "force", required = false) Boolean force) {
        return ApiResult.success(updateCenterApplicationService.getReleaseDetail(version, force));
    }

    /**
     * 查询Git提交日志
     * 接口编号：API000524
     * 产品文档功能：管理 / 更新中心 - Git提交记录
     */
    @Operation(summary = "查询Git提交日志", description = "接口编号：API000524")
    @GetMapping("/github-logs")
    public ApiResult<UpdateCenterOutput.GitHubLogsOutput> getGitHubLogs(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "before", required = false) String before,
            @RequestParam(value = "force", required = false) Boolean force) {
        return ApiResult.success(updateCenterApplicationService.getGitHubLogs(limit, before, force));
    }
}
