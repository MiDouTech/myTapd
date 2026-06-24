package com.miduo.cloud.ticket.controller.integration;

import com.miduo.cloud.ticket.application.integration.IntegrationAppApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppCreateInput;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppOutput;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppPageInput;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppRotateSecretOutput;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppUpdateInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 接入应用管理
 */
@Tag(name = "接入应用", description = "业务原生工单插件接入应用管理")
@RestController
@RequestMapping("/api/integration/app")
public class IntegrationAppController {

    private final IntegrationAppApplicationService integrationAppApplicationService;

    public IntegrationAppController(IntegrationAppApplicationService integrationAppApplicationService) {
        this.integrationAppApplicationService = integrationAppApplicationService;
    }

    /**
     * 接入应用分页列表
     * 接口编号：API000527
     */
    @Operation(summary = "接入应用分页列表", description = "接口编号：API000527")
    @GetMapping("/page")
    public ApiResult<PageOutput<IntegrationAppOutput>> page(@Valid IntegrationAppPageInput input) {
        return ApiResult.success(integrationAppApplicationService.page(input));
    }

    /**
     * 接入应用详情
     */
    @Operation(summary = "接入应用详情")
    @GetMapping("/detail/{id}")
    public ApiResult<IntegrationAppOutput> detail(@PathVariable Long id) {
        return ApiResult.success(integrationAppApplicationService.detail(id));
    }

    /**
     * 创建接入应用
     * 接口编号：API000528
     */
    @OperationLog(moduleName = "接入应用", operationItem = "创建接入应用")
    @Operation(summary = "创建接入应用", description = "接口编号：API000528")
    @PostMapping("/create")
    public ApiResult<Long> create(@Valid @RequestBody IntegrationAppCreateInput input) {
        return ApiResult.success(integrationAppApplicationService.create(input));
    }

    /**
     * 更新接入应用
     * 接口编号：API000529
     */
    @OperationLog(moduleName = "接入应用", operationItem = "更新接入应用")
    @Operation(summary = "更新接入应用", description = "接口编号：API000529")
    @PutMapping("/update/{id}")
    public ApiResult<Void> update(@PathVariable Long id,
                                  @Valid @RequestBody IntegrationAppUpdateInput input) {
        integrationAppApplicationService.update(id, input);
        return ApiResult.success();
    }

    /**
     * 轮换 AppSecret
     * 接口编号：API000530
     */
    @OperationLog(moduleName = "接入应用", operationItem = "轮换AppSecret")
    @Operation(summary = "轮换AppSecret", description = "接口编号：API000530")
    @PostMapping("/rotate-secret/{id}")
    public ApiResult<IntegrationAppRotateSecretOutput> rotateSecret(@PathVariable Long id) {
        return ApiResult.success(integrationAppApplicationService.rotateSecret(id));
    }
}
