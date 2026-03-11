package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 缺陷变更历史条目响应
 * 接口编号：API000501（GET /api/ticket/{ticketId}/change-history）
 */
@Data
public class BugChangeHistoryOutput implements Serializable {

    /**
     * ticket_log 主键 ID
     */
    private Long id;

    /**
     * 展示序号（按时间倒序，最新操作序号为 1）
     */
    private Integer seq;

    /**
     * 变更时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String changeTime;

    /**
     * 变更人用户ID
     */
    private Long changeByUserId;

    /**
     * 变更人姓名（系统自动变更时为"系统"）
     */
    private String changeByUserName;

    /**
     * 变更人头像 URL
     */
    private String changeByAvatar;

    /**
     * 变更类型 code（对应 BugChangeTypeEnum.code）
     */
    private String changeType;

    /**
     * 变更类型中文标签（对应 BugChangeTypeEnum.label）
     */
    private String changeTypeLabel;

    /**
     * 本次操作涉及的字段变更明细列表
     */
    private List<BugFieldChangeItem> fields;
}
