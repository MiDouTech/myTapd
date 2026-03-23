package com.miduo.cloud.ticket.controller.wecom;

import com.miduo.cloud.ticket.application.wecom.WecomGroupBindingApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomGroupBindingCreateInput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomGroupBindingListOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomGroupBindingUpdateInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 企微群绑定配置管理
 */
@Tag(name = "企微群绑定", description = "企微群与工单分类绑定配置")
@RestController
@RequestMapping("/api/wecom/group-binding")
public class WecomGroupBindingController {

    private final WecomGroupBindingApplicationService bindingService;

    public WecomGroupBindingController(WecomGroupBindingApplicationService bindingService) {
        this.bindingService = bindingService;
    }

    /**
     * 查询群绑定配置列表
     * 接口编号：API000021
     * 产品文档功能：4.6.4 企微群机器人工单 - 群与工单分类绑定管理
     */
    @Operation(summary = "查询群绑定配置列表", description = "接口编号：API000021")
    @GetMapping("/list")
    public ApiResult<List<WecomGroupBindingListOutput>> listBindings() {
        return ApiResult.success(bindingService.listBindings());
    }

    /**
     * 新增群绑定配置
     * 接口编号：API000022
     * 产品文档功能：4.6.4 企微群机器人工单 - 群绑定配置新增
     */
    @OperationLog(moduleName = "企微群绑定", operationItem = "新增群绑定配置")
    @Operation(summary = "新增群绑定配置", description = "接口编号：API000022")
    @PostMapping("/create")
    public ApiResult<Long> createBinding(@Valid @RequestBody WecomGroupBindingCreateInput input) {
        Long id = bindingService.createBinding(input);
        return ApiResult.success(id);
    }

    /**
     * 修改群绑定配置
     * 接口编号：API000023
     * 产品文档功能：4.6.4 企微群机器人工单 - 群绑定配置修改
     */
    @OperationLog(moduleName = "企微群绑定", operationItem = "修改群绑定配置")
    @Operation(summary = "修改群绑定配置", description = "接口编号：API000023")
    @PutMapping("/update/{id}")
    public ApiResult<Void> updateBinding(@PathVariable("id") Long id,
                                         @Valid @RequestBody WecomGroupBindingUpdateInput input) {
        bindingService.updateBinding(id, input);
        return ApiResult.success();
    }
}
