package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.KanbanApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.ticket.KanbanColumnOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.KanbanMoveInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 工单看板控制器
 */
@Tag(name = "工单看板", description = "看板视图与拖拽流转")
@RestController
@RequestMapping("/api/ticket/kanban")
public class KanbanController {

    private final KanbanApplicationService kanbanService;

    public KanbanController(KanbanApplicationService kanbanService) {
        this.kanbanService = kanbanService;
    }

    /**
     * 获取看板数据
     * 接口编号：API000411
     * 产品文档功能：5.3 看板视图 - 按状态分列展示工单
     */
    @Operation(summary = "获取看板数据", description = "接口编号：API000411")
    @GetMapping
    public ApiResult<List<KanbanColumnOutput>> getKanban(@RequestParam(required = false) Integer limit) {
        return ApiResult.success(kanbanService.getKanbanData(limit));
    }

    /**
     * 看板拖拽变更状态
     * 接口编号：API000412
     * 产品文档功能：5.3 看板视图 - 拖拽卡片变更状态
     */
    @Operation(summary = "看板拖拽变更状态", description = "接口编号：API000412")
    @PutMapping("/move")
    public ApiResult<Void> move(@Valid @RequestBody KanbanMoveInput input) {
        kanbanService.moveTicket(input, getCurrentUserId());
        return ApiResult.success();
    }

    private Long getCurrentUserId() {
        return 1L;
    }
}
