package com.miduo.cloud.ticket.common.util;

/**
 * 企微群工单事件通知紧凑单行格式：
 * 【工单编号】【状态】【标题】详情链接
 */
public final class TicketWecomCompactNotificationFormat {

    private TicketWecomCompactNotificationFormat() {
    }

    public static String build(String ticketNo, String statusLabel, String title, String detailLink) {
        StringBuilder line = new StringBuilder();
        line.append("【").append(safeSegment(ticketNo)).append("】");
        line.append("【").append(safeSegment(statusLabel)).append("】");
        line.append("【").append(safeSegment(title)).append("】");
        if (detailLink != null) {
            String link = detailLink.trim();
            if (!link.isEmpty()) {
                line.append(link);
            }
        }
        return line.toString();
    }

    private static String safeSegment(String value) {
        if (value == null) {
            return "-";
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? "-" : normalized;
    }
}
