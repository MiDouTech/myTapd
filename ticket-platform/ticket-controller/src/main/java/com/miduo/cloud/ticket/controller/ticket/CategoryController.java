package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.category.CategoryApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.category.CategoryCreateInput;
import com.miduo.cloud.ticket.entity.dto.category.CategoryDetailOutput;
import com.miduo.cloud.ticket.entity.dto.category.CategoryTreeOutput;
import com.miduo.cloud.ticket.entity.dto.category.CategoryUpdateInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/category")
@Tag(name = "分类管理", description = "工单分类管理接口")
public class CategoryController {

    @Resource
    private CategoryApplicationService categoryService;

    /**
     * 获取三级分类树
     * 接口编号：API000001
     * 产品文档功能：4.3.1 工单分类体系 - 三级分类树展示
     */
    @GetMapping("/tree")
    @Operation(summary = "获取三级分类树", description = "接口编号：API000001")
    public ApiResult<List<CategoryTreeOutput>> getCategoryTree() {
        List<CategoryTreeOutput> tree = categoryService.getCategoryTree();
        return ApiResult.success(tree);
    }

    /**
     * 获取分类详情
     * 接口编号：API000002
     * 产品文档功能：4.3.1 工单分类体系 - 分类详情查看
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取分类详情", description = "接口编号：API000002")
    public ApiResult<CategoryDetailOutput> getCategoryDetail(@PathVariable Long id) {
        CategoryDetailOutput detail = categoryService.getCategoryDetail(id);
        return ApiResult.success(detail);
    }

    /**
     * 新增分类
     * 接口编号：API000003
     * 产品文档功能：4.3.1 工单分类体系 - 新增分类
     */
    @PostMapping("/create")
    @Operation(summary = "新增分类", description = "接口编号：API000003")
    public ApiResult<Long> createCategory(@Valid @RequestBody CategoryCreateInput input) {
        Long id = categoryService.createCategory(input);
        return ApiResult.success(id);
    }

    /**
     * 修改分类
     * 接口编号：API000004
     * 产品文档功能：4.3.1 工单分类体系 - 修改分类
     */
    @PutMapping("/update/{id}")
    @Operation(summary = "修改分类", description = "接口编号：API000004")
    public ApiResult<Void> updateCategory(@PathVariable Long id,
                                          @Valid @RequestBody CategoryUpdateInput input) {
        categoryService.updateCategory(id, input);
        return ApiResult.success();
    }

    /**
     * 删除分类
     * 接口编号：API000422
     * 产品文档功能：4.3.1 工单分类体系 - 删除分类
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除分类", description = "接口编号：API000422")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResult.success();
    }
}
