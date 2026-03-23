package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.TicketModuleApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketModuleInput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketModuleOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 工单模块管理 Controller（为测试信息-所属模块提供自定义下拉选项）
 */
@RestController
@RequestMapping("/api/ticket-module")
@Tag(name = "工单模块管理", description = "工单测试信息-所属模块自定义选项管理")
public class TicketModuleController {

    @Resource
    private TicketModuleApplicationService ticketModuleApplicationService;

    /**
     * 获取工单模块列表
     * 接口编号：API000505
     * 产品文档功能：测试信息 - 所属模块下拉选项列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取工单模块列表", description = "接口编号：API000505")
    public ApiResult<List<TicketModuleOutput>> listModules() {
        return ApiResult.success(ticketModuleApplicationService.listModules());
    }

    /**
     * 创建工单模块
     * 接口编号：API000506
     * 产品文档功能：测试信息 - 新增所属模块选项
     */
    @OperationLog(moduleName = "工单模块管理", operationItem = "创建工单模块")
    @PostMapping("/create")
    @Operation(summary = "创建工单模块", description = "接口编号：API000506")
    public ApiResult<Long> createModule(@Valid @RequestBody TicketModuleInput input) {
        return ApiResult.success(ticketModuleApplicationService.createModule(input));
    }

    /**
     * 删除工单模块
     * 接口编号：API000507
     * 产品文档功能：测试信息 - 删除所属模块选项
     */
    @OperationLog(moduleName = "工单模块管理", operationItem = "删除工单模块", recordParams = false)
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除工单模块", description = "接口编号：API000507")
    public ApiResult<Void> deleteModule(@PathVariable Long id) {
        ticketModuleApplicationService.deleteModule(id);
        return ApiResult.success();
    }
}
