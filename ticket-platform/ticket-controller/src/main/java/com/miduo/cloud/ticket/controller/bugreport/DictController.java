package com.miduo.cloud.ticket.controller.bugreport;

import com.miduo.cloud.ticket.application.bugreport.DictApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.bugreport.DefectCategoryOutput;
import com.miduo.cloud.ticket.entity.dto.bugreport.LogicCauseTreeOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bug简报字典接口
 */
@RestController
@RequestMapping("/api/dict")
@Tag(name = "Bug简报字典", description = "逻辑归因和缺陷分类字典")
public class DictController {

    private final DictApplicationService dictApplicationService;

    public DictController(DictApplicationService dictApplicationService) {
        this.dictApplicationService = dictApplicationService;
    }

    /**
     * 逻辑归因字典树
     * 接口编号：API000029
     * 产品文档功能：4.12 Bug简报管理 - 逻辑归因字典
     */
    @GetMapping("/logic-cause")
    @Operation(summary = "逻辑归因字典树", description = "接口编号：API000029")
    public ApiResult<List<LogicCauseTreeOutput>> logicCause() {
        return ApiResult.success(dictApplicationService.getLogicCauseTree());
    }

    /**
     * 缺陷分类字典
     * 接口编号：API000030
     * 产品文档功能：4.12 Bug简报管理 - 缺陷分类字典
     */
    @GetMapping("/defect-category")
    @Operation(summary = "缺陷分类字典", description = "接口编号：API000030")
    public ApiResult<List<DefectCategoryOutput>> defectCategory() {
        return ApiResult.success(dictApplicationService.getDefectCategories());
    }
}
