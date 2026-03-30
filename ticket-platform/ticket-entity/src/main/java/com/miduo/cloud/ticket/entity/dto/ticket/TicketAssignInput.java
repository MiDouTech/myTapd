package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TicketAssignInput implements Serializable {

    /**
     * 单个处理人（兼容旧客户端）
     */
    private Long assigneeId;

    /**
     * 多名处理人，首位为主处理人；与 assigneeId 至少填一种
     */
    private List<Long> assigneeIds;

    private String remark;

    /**
     * true：在现有处理人基础上追加协同人（去重），首位主处理人不变；用于测试复现中联调开发等场景
     */
    private Boolean mergeAssignees;
}
