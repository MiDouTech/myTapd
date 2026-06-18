package com.miduo.cloud.ticket.common.util;

/**
 * 企微群工单事件通知紧凑单行格式：
 * - 工单事件：【工单编号】【状态】【标题】详情链接
 * - 评论@：【工单编号】【评论人@你】【评论摘要】详情链接
 */
public final class TicketWecomCompactNotificationFormat {

    /** 评论@摘要在群消息中的最大展示字符数（超出截断） */
    public static final int COMMENT_MENTION_SUMMARY_DISPLAY_MAX = 50;

    private static final String EMPTY_COMMENT_SUMMARY = "（无文字评论）";
    private static final String UNKNOWN_AUTHOR_MENTION = "有人@你";

    private TicketWecomCompactNotificationFormat() {
    }

    public static String build(String ticketNo, String statusLabel, String title, String detailLink) {
        StringBuilder line = new StringBuilder();
        line.append("【").append(safeSegment(ticketNo)).append("】");
        line.append("【").append(safeSegment(statusLabel)).append("】");
        line.append("【").append(safeSegment(title)).append("】");
        appendDetailLink(line, detailLink);
        return line.toString();
    }

    public static String buildCommentMention(String ticketNo,
                                             String authorName,
                                             String commentSummary,
                                             String detailLink) {
        StringBuilder line = new StringBuilder();
        line.append("【").append(safeSegment(ticketNo)).append("】");
        line.append("【").append(formatAuthorMention(authorName)).append("】");
        line.append("【").append(formatCommentSummary(commentSummary)).append("】");
        appendDetailLink(line, detailLink);
        return line.toString();
    }

    private static String formatAuthorMention(String authorName) {
        if (authorName == null) {
            return UNKNOWN_AUTHOR_MENTION;
        }
        String normalized = authorName.trim();
        if (normalized.isEmpty() || "-".equals(normalized)) {
            return UNKNOWN_AUTHOR_MENTION;
        }
        return normalized + "@你";
    }

    private static String formatCommentSummary(String commentSummary) {
        if (commentSummary == null) {
            return EMPTY_COMMENT_SUMMARY;
        }
        String normalized = commentSummary.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return EMPTY_COMMENT_SUMMARY;
        }
        if (normalized.length() <= COMMENT_MENTION_SUMMARY_DISPLAY_MAX) {
            return normalized;
        }
        return normalized.substring(0, COMMENT_MENTION_SUMMARY_DISPLAY_MAX) + "…";
    }

    private static void appendDetailLink(StringBuilder line, String detailLink) {
        if (detailLink == null) {
            return;
        }
        String link = detailLink.trim();
        if (!link.isEmpty()) {
            line.append(link);
        }
    }

    private static String safeSegment(String value) {
        if (value == null) {
            return "-";
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? "-" : normalized;
    }
}
