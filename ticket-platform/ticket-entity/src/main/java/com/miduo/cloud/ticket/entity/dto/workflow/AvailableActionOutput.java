package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 可用操作输出
 */
@Data
public class AvailableActionOutput implements Serializable {

    private Long ticketId;
    private String currentStatus;
    private String currentStatusName;
    private List<ActionItem> actions;

    @Data
    public static class ActionItem implements Serializable {
        private String targetStatus;
        private String targetStatusName;
        private String actionName;
        private List<String> allowedRoles;
    }
}
