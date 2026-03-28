package com.miduo.cloud.ticket.application.wecom;

/**
 * 企微工单公开链接构建器。
 */
public final class WecomPublicLinkBuilder {

    private static final String CALLBACK_PATH = "/api/wecom/callback";

    private WecomPublicLinkBuilder() {
    }

    /**
     * 构建工单公开详情链接。
     */
    public static String buildPublicTicketLink(String trustedDomain, String ticketNo) {
        if (ticketNo == null || ticketNo.trim().isEmpty()) {
            return "-";
        }
        if (trustedDomain == null || trustedDomain.trim().isEmpty()) {
            return "-";
        }

        String normalizedDomain = trustedDomain.trim();
        if (!normalizedDomain.startsWith("http://") && !normalizedDomain.startsWith("https://")) {
            normalizedDomain = "https://" + normalizedDomain;
        }

        normalizedDomain = trimTrailingSlash(normalizedDomain);

        int callbackIndex = normalizedDomain.toLowerCase().indexOf(CALLBACK_PATH);
        if (callbackIndex >= 0) {
            // 为什么要截断：历史配置把回调路径写进了域名，直接拼接会变成错误链接并导致404。
            normalizedDomain = normalizedDomain.substring(0, callbackIndex);
        }
        normalizedDomain = trimTrailingSlash(normalizedDomain);

        return normalizedDomain + "/open/ticket/" + ticketNo.trim();
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
