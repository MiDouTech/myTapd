package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.customfield.CustomFieldApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.customfield.CustomFieldCreateInput;
import com.miduo.cloud.ticket.entity.dto.customfield.CustomFieldOutput;
import com.miduo.cloud.ticket.entity.dto.customfield.CustomFieldUpdateInput;
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

@RestController
@RequestMapping("/api/custom-field")
@Tag(name = "自定义字段管理", description = "自定义字段定义的查询、创建、更新和删除")
public class CustomFieldController {

    private final CustomFieldApplicationService customFieldService;

    public CustomFieldController(CustomFieldApplicationService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @GetMapping("/list")
    @Operation(summary = "查询自定义字段列表")
    public ApiResult<List<CustomFieldOutput>> listFields() {
        return ApiResult.success(customFieldService.listFields());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询自定义字段详情")
    public ApiResult<CustomFieldOutput> getField(@PathVariable Long id) {
        return ApiResult.success(customFieldService.getField(id));
    }

    @OperationLog(moduleName = "自定义字段", operationItem = "创建自定义字段")
    @PostMapping("/create")
    @Operation(summary = "创建自定义字段")
    public ApiResult<CustomFieldOutput> createField(@Valid @RequestBody CustomFieldCreateInput input) {
        return ApiResult.success(customFieldService.createField(input));
    }

    @OperationLog(moduleName = "自定义字段", operationItem = "更新自定义字段")
    @PutMapping("/update")
    @Operation(summary = "更新自定义字段")
    public ApiResult<CustomFieldOutput> updateField(@Valid @RequestBody CustomFieldUpdateInput input) {
        return ApiResult.success(customFieldService.updateField(input));
    }

    @OperationLog(moduleName = "自定义字段", operationItem = "删除自定义字段", recordParams = false)
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除自定义字段")
    public ApiResult<Void> deleteField(@PathVariable Long id) {
        customFieldService.deleteField(id);
        return ApiResult.success();
    }
}
