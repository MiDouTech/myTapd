package com.miduo.cloud.ticket.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 工单详情链接相关配置（与 Webhook 等使用的 ticket.detail-url 对齐，并支持 SPA 工单详情路径）
 */
@Data
@Component
@ConfigurationProperties(prefix = "ticket")
public class TicketLinkProperties {

    /**
     * 工单详情基础 URL（如 Webhook 正文中的「详情」链接：base + "/" + ticketNo）
     */
    private String detailUrl = "";

    /**
     * 内部 SPA 前端根地址（不含路径尾斜杠），用于拼接 /ticket/detail/{id}，供企微卡片与邮件「查看详情」
     */
    private String spaDetailBaseUrl = "";

    /**
     * 生成可点击的工单详情链接：优先 SPA（按数字 id），否则回退到 detail-url + "/" + ticketNo
     */
    public String buildDetailLink(Long ticketId, String ticketNo) {
        if (ticketId != null && ticketId > 0 && StringUtils.hasText(spaDetailBaseUrl)) {
            return stripTrailingSlash(spaDetailBaseUrl.trim()) + "/ticket/detail/" + ticketId;
        }
        if (StringUtils.hasText(detailUrl) && StringUtils.hasText(ticketNo)) {
            return stripTrailingSlash(detailUrl.trim()) + "/" + ticketNo.trim();
        }
        return "";
    }

    private static String stripTrailingSlash(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') {
            end--;
        }
        return end == s.length() ? s : s.substring(0, end);
    }
}
