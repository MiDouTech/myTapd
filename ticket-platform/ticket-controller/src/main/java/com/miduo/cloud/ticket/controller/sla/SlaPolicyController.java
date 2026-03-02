package com.miduo.cloud.ticket.controller.sla;

import com.miduo.cloud.ticket.application.sla.SlaApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.sla.SlaPolicyCreateInput;
import com.miduo.cloud.ticket.entity.dto.sla.SlaPolicyOutput;
import com.miduo.cloud.ticket.entity.dto.sla.SlaPolicyUpdateInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * SLA策略管理控制器
 */
@Tag(name = "SLA策略管理", description = "SLA策略的查询、创建和更新")
@RestController
@RequestMapping("/api/sla/policy")
public class SlaPolicyController {

    private final SlaApplicationService slaApplicationService;

    public SlaPolicyController(SlaApplicationService slaApplicationService) {
        this.slaApplicationService = slaApplicationService;
    }

    /**
     * 查询SLA策略列表
     * 接口编号：API000001
     * 产品文档功能：4.7 SLA管理 - 策略列表查询
     */
    @Operation(summary = "查询SLA策略列表", description = "接口编号：API000001")
    @GetMapping("/list")
    public ApiResult<List<SlaPolicyOutput>> listPolicies() {
        List<SlaPolicyOutput> result = slaApplicationService.listPolicies();
        return ApiResult.success(result);
    }

    /**
     * 创建SLA策略
     * 接口编号：API000002
     * 产品文档功能：4.7 SLA管理 - 新增SLA策略
     */
    @Operation(summary = "创建SLA策略", description = "接口编号：API000002")
    @PostMapping("/create")
    public ApiResult<SlaPolicyOutput> createPolicy(@Valid @RequestBody SlaPolicyCreateInput input) {
        SlaPolicyOutput result = slaApplicationService.createPolicy(input);
        return ApiResult.success(result);
    }

    /**
     * 更新SLA策略
     * 接口编号：API000003
     * 产品文档功能：4.7 SLA管理 - 更新SLA策略
     */
    @Operation(summary = "更新SLA策略", description = "接口编号：API000003")
    @PutMapping("/update")
    public ApiResult<SlaPolicyOutput> updatePolicy(@Valid @RequestBody SlaPolicyUpdateInput input) {
        SlaPolicyOutput result = slaApplicationService.updatePolicy(input);
        return ApiResult.success(result);
    }
}
