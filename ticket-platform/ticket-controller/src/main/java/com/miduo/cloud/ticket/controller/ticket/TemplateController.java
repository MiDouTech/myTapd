package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.template.TemplateApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.template.TemplateListOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
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
}
