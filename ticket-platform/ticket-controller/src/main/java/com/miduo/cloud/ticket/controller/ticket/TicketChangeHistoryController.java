package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.TicketChangeHistoryApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.ticket.BugChangeHistoryOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 缺陷变更历史控制器
 * 提供缺陷工单的字段级变更历史查询能力
 */
@RestController
@RequestMapping("/api/ticket")
@Tag(name = "缺陷变更历史", description = "缺陷工单变更历史查询接口")
public class TicketChangeHistoryController {

    @Resource
    private TicketChangeHistoryApplicationService changeHistoryService;

    /**
     * 查询缺陷变更历史列表
     * 接口编号：API000501
     * 产品文档功能：缺陷详情-变更历史Tab（PRD §3.4）
     *
     * @param ticketId   工单 ID（路径参数）
     * @param changeType 变更类型筛选（可选，BugChangeTypeEnum.code）
     * @param fieldName  变更字段筛选（可选，英文字段名，内存过滤）
     */
    @GetMapping("/{ticketId}/change-history")
    @Operation(summary = "查询缺陷变更历史", description = "接口编号：API000501")
    public ApiResult<List<BugChangeHistoryOutput>> listChangeHistory(
            @PathVariable Long ticketId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String fieldName) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        List<BugChangeHistoryOutput> result =
                changeHistoryService.listChangeHistory(ticketId, changeType, fieldName);
        return ApiResult.success(result);
    }
}
