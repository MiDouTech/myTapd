package com.miduo.cloud.ticket.entity.dto.ticket;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 缺陷变更历史查询请求
 * 接口编号：API000501（GET /api/ticket/{ticketId}/change-history）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BugChangeHistoryPageInput extends PageInput {

    /**
     * 工单 ID（必填）
     */
    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    /**
     * 变更类型筛选（对应 BugChangeTypeEnum.code），null 表示全部
     * 可选值：CREATE / MANUAL_CHANGE / STATUS_CHANGE / SYSTEM_AUTO / COMMENT / ATTACHMENT
     */
    private String changeType;

    /**
     * 变更字段筛选（英文字段名），null 表示全部
     * 该筛选在 Service 层内存完成，不下推 SQL，避免 JSON 函数全表扫描
     */
    private String fieldName;
}
