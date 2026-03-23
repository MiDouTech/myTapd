package com.miduo.cloud.ticket.controller.wecom;

import com.miduo.cloud.ticket.application.wecom.WecomConfigApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomConfigOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomConfigUpdateInput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomConnectionTestOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 企业微信配置管理接口
 */
@Tag(name = "企业微信配置", description = "企业微信连接配置与连接测试")
@RestController
@RequestMapping("/api/wecom/config")
public class WecomConfigController {

    private final WecomConfigApplicationService wecomConfigApplicationService;

    public WecomConfigController(WecomConfigApplicationService wecomConfigApplicationService) {
        this.wecomConfigApplicationService = wecomConfigApplicationService;
    }

    /**
     * 查询企业微信配置详情
     * 接口编号：API000422
     * 产品文档功能：SSO一期-企微配置管理
     */
    @Operation(summary = "查询企业微信配置详情", description = "接口编号：API000422")
    @GetMapping("/detail")
    public ApiResult<WecomConfigOutput> detail() {
        return ApiResult.success(wecomConfigApplicationService.detail());
    }

    /**
     * 保存企业微信配置
     * 接口编号：API000423
     * 产品文档功能：SSO一期-企微配置管理
     */
    @OperationLog(moduleName = "企业微信配置", operationItem = "保存企业微信配置")
    @Operation(summary = "保存企业微信配置", description = "接口编号：API000423")
    @PostMapping("/save")
    public ApiResult<Long> save(@Valid @RequestBody WecomConfigUpdateInput input) {
        return ApiResult.success(wecomConfigApplicationService.saveOrUpdate(input));
    }

    /**
     * 连接测试
     * 接口编号：API000424
     * 产品文档功能：SSO一期-连接测试
     */
    @OperationLog(moduleName = "企业微信配置", operationItem = "企业微信连接测试", recordParams = false)
    @Operation(summary = "企业微信连接测试", description = "接口编号：API000424")
    @PostMapping("/test-connect")
    public ApiResult<WecomConnectionTestOutput> testConnect() {
        return ApiResult.success(wecomConfigApplicationService.testConnection());
    }
}
