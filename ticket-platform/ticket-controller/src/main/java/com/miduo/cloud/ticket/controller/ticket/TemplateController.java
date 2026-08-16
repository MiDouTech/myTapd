package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.template.TemplateApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.template.TemplateListOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateAdminListOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateCreateInput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateDetailOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateUpdateInput;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/template")
@Tag(name = "模板管理", description = "工单模板管理接口")
public class TemplateController {

    @Resource
    private TemplateApplicationService templateService;

    /**
     * 按分类查询模板列表
     * 接口编号：API000005
     * 产品文档功能：4.3.2 工单模板与自定义字段 - 模板列表
     */
    @GetMapping("/list")
    @Operation(summary = "按分类查询模板列表", description = "接口编号：API000005")
    public ApiResult<List<TemplateListOutput>> getTemplateList(
            @RequestParam(required = false) Long categoryId) {
        List<TemplateListOutput> list = templateService.getTemplateList(categoryId);
        return ApiResult.success(list);
    }

    @GetMapping("/admin/list")
    @Operation(summary = "查询模板管理列表")
    public ApiResult<List<TemplateAdminListOutput>> getAdminList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isActive) {
        return ApiResult.success(templateService.getAdminList(keyword, isActive));
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "查询模板详情")
    public ApiResult<TemplateDetailOutput> getDetail(@PathVariable Long id) {
        return ApiResult.success(templateService.getDetail(id));
    }

    @OperationLog(moduleName = "模板设置", operationItem = "创建模板")
    @PostMapping("/admin/create")
    @Operation(summary = "创建模板")
    public ApiResult<TemplateDetailOutput> create(@Valid @RequestBody TemplateCreateInput input) {
        return ApiResult.success(templateService.createTemplate(input));
    }

    @OperationLog(moduleName = "模板设置", operationItem = "更新模板")
    @PutMapping("/admin/update")
    @Operation(summary = "更新模板")
    public ApiResult<TemplateDetailOutput> update(@Valid @RequestBody TemplateUpdateInput input) {
        return ApiResult.success(templateService.updateTemplate(input));
    }

    @OperationLog(moduleName = "模板设置", operationItem = "删除模板", recordParams = false)
    @DeleteMapping("/admin/delete/{id}")
    @Operation(summary = "删除模板")
    public ApiResult<Void> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ApiResult.success();
    }
}
