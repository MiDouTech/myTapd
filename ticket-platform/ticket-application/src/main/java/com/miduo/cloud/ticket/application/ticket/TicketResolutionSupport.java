package com.miduo.cloud.ticket.application.ticket;

import org.springframework.util.StringUtils;

/**
 * 处理结论字段归一与合并（关闭/终态流转时写入 ticket.resolution_summary）
 */
public final class TicketResolutionSupport {

    public static final int MAX_LENGTH = 2000;

    private TicketResolutionSupport() {
    }

    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String t = raw.trim();
        if (t.length() > MAX_LENGTH) {
            t = t.substring(0, MAX_LENGTH);
        }
        return t;
    }

    /**
     * explicitResolution 优先；否则用 remark；若已有库内结论则与新区块合并（去简单重复）
     */
    public static String merge(String existingDb, String remark, String explicitResolution) {
        String explicit = normalize(explicitResolution);
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        String rem = normalize(remark);
        String exist = normalize(existingDb);
        if (!StringUtils.hasText(rem)) {
            return exist;
        }
        if (!StringUtils.hasText(exist)) {
            return rem;
        }
        if (exist.contains(rem) || rem.contains(exist)) {
            return exist.length() >= rem.length() ? exist : rem;
        }
        return exist + "\n---\n" + rem;
    }
}
