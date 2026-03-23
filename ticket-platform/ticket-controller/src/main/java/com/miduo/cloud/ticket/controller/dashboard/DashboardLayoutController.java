package com.miduo.cloud.ticket.controller.dashboard;

import com.miduo.cloud.ticket.application.dashboard.DashboardLayoutApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardLayoutItemOutput;
import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardLayoutSaveInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 仪表盘个人布局控制器
 * 提供用户个人仪表盘行组排序配置的读写接口
 */
@Tag(name = "仪表盘个人布局", description = "获取/保存/恢复用户个人仪表盘布局")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardLayoutController {

    private final DashboardLayoutApplicationService layoutService;

    public DashboardLayoutController(DashboardLayoutApplicationService layoutService) {
        this.layoutService = layoutService;
    }

    /**
     * 获取当前用户个人仪表盘布局配置
     * 接口编号：API000411
     * 产品文档功能：仪表盘个性化布局 §3.1
     */
    @Operation(summary = "获取个人仪表盘布局", description = "接口编号：API000411")
    @GetMapping("/layout")
    public ApiResult<List<DashboardLayoutItemOutput>> getLayout() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return ApiResult.success(layoutService.getLayout(userId));
    }

    /**
     * 保存当前用户个人仪表盘布局配置
     * 接口编号：API000412
     * 产品文档功能：仪表盘个性化布局 §3.4
     */
    @OperationLog(moduleName = "仪表盘个人布局", operationItem = "保存个人仪表盘布局")
    @Operation(summary = "保存个人仪表盘布局", description = "接口编号：API000412")
    @PutMapping("/layout")
    public ApiResult<Void> saveLayout(@RequestBody @Valid DashboardLayoutSaveInput input) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        layoutService.saveLayout(userId, input);
        return ApiResult.success();
    }

    /**
     * 恢复当前用户仪表盘为系统默认布局
     * 接口编号：API000413
     * 产品文档功能：仪表盘个性化布局 §3.6
     */
    @OperationLog(moduleName = "仪表盘个人布局", operationItem = "恢复默认仪表盘布局", recordParams = false)
    @Operation(summary = "恢复默认仪表盘布局", description = "接口编号：API000413")
    @DeleteMapping("/layout")
    public ApiResult<Void> resetLayout() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        layoutService.resetLayout(userId);
        return ApiResult.success();
    }
}
