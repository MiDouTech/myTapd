package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.TicketSavedQueryApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketSavedQueryOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketSavedQuerySaveInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "个人常用查询", description = "当前登录用户的工单查询与导出字段配置")
@RestController
@RequestMapping("/api/ticket/saved-query")
public class TicketSavedQueryController {
    private final TicketSavedQueryApplicationService service;

    public TicketSavedQueryController(TicketSavedQueryApplicationService service) {
        this.service = service;
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前用户的常用查询")
    public ApiResult<List<TicketSavedQueryOutput>> list() {
        return ApiResult.success(service.list(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping
    @OperationLog(moduleName = "个人常用查询", operationItem = "新增常用查询")
    @Operation(summary = "新增当前用户的常用查询")
    public ApiResult<TicketSavedQueryOutput> create(@Valid @RequestBody TicketSavedQuerySaveInput input) {
        return ApiResult.success(service.create(SecurityUtil.getCurrentUserId(), input));
    }

    @PutMapping("/{id}")
    @OperationLog(moduleName = "个人常用查询", operationItem = "更新常用查询")
    @Operation(summary = "更新当前用户的常用查询")
    public ApiResult<TicketSavedQueryOutput> update(@PathVariable Long id,
                                                    @Valid @RequestBody TicketSavedQuerySaveInput input) {
        return ApiResult.success(service.update(SecurityUtil.getCurrentUserId(), id, input));
    }

    @DeleteMapping("/{id}")
    @OperationLog(moduleName = "个人常用查询", operationItem = "删除常用查询", recordParams = false)
    @Operation(summary = "删除当前用户的常用查询")
    public ApiResult<Void> delete(@PathVariable Long id) {
        service.delete(SecurityUtil.getCurrentUserId(), id);
        return ApiResult.success();
    }
}
