package com.miduo.cloud.ticket.controller.system;

import com.miduo.cloud.ticket.application.system.BasicSettingsApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.system.BasicSettingsOutput;
import com.miduo.cloud.ticket.entity.dto.system.BasicSettingsUpdateInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 系统基础参数配置控制器
 */
@Tag(name = "系统配置", description = "基础参数配置管理")
@RestController
@RequestMapping("/api/system-config")
public class SystemConfigController {

    private final BasicSettingsApplicationService basicSettingsService;

    public SystemConfigController(BasicSettingsApplicationService basicSettingsService) {
        this.basicSettingsService = basicSettingsService;
    }

    /**
     * 查询基础参数配置
     * 接口编号：API000505
     * 产品文档功能：系统设置 - 基础参数查询
     */
    @Operation(summary = "查询基础参数配置", description = "接口编号：API000505")
    @GetMapping("/basic")
    public ApiResult<BasicSettingsOutput> getBasicSettings() {
        return ApiResult.success(basicSettingsService.getBasicSettings());
    }

    /**
     * 更新基础参数配置
     * 接口编号：API000506
     * 产品文档功能：系统设置 - 基础参数更新
     */
    @Operation(summary = "更新基础参数配置", description = "接口编号：API000506")
    @PutMapping("/basic")
    public ApiResult<Void> updateBasicSettings(@Valid @RequestBody BasicSettingsUpdateInput input) {
        basicSettingsService.updateBasicSettings(input);
        return ApiResult.success();
    }
}
