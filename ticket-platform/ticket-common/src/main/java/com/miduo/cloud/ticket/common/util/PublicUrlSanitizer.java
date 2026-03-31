package com.miduo.cloud.ticket.common.util;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 公开详情中 URL 脱敏：移除常见敏感 query 参数名（大小写不敏感）
 */
public final class PublicUrlSanitizer {

    private static final Set<String> SENSITIVE_QUERY_KEYS = new HashSet<>(Arrays.asList(
            "access_token", "token", "refresh_token", "password", "passwd", "secret",
            "authorization", "auth", "session", "sessionid", "jsessionid", "api_key", "apikey"));

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private PublicUrlSanitizer() {
    }

    /**
     * 对可能含 HTML 的文本做极简消毒：去掉标签，保留纯文本（用于公开接口防 XSS）
     */
    public static String stripHtmlTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        return HTML_TAG.matcher(raw).replaceAll(" ").trim();
    }

    public static String sanitizeUrlForPublic(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String s = raw.trim();
        if (s.length() > 1000) {
            s = s.substring(0, 1000);
        }
        try {
            URI uri = URI.create(s);
            if (uri.getScheme() != null && uri.getHost() == null && !s.startsWith("http://") && !s.startsWith("https://")) {
                return maskSensitiveQueryInPathStyle(s);
            }
            String query = uri.getRawQuery();
            if (!StringUtils.hasText(query)) {
                return s;
            }
            String[] pairs = query.split("&");
            StringBuilder qb = new StringBuilder();
            for (String pair : pairs) {
                if (!StringUtils.hasText(pair)) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String keyEnc = eq >= 0 ? pair.substring(0, eq) : pair;
                String key = urlDecode(keyEnc).toLowerCase(Locale.ROOT);
                if (SENSITIVE_QUERY_KEYS.contains(key)) {
                    continue;
                }
                if (qb.length() > 0) {
                    qb.append('&');
                }
                qb.append(pair);
            }
            String newQuery = qb.toString();
            String base = s;
            int qIdx = base.indexOf('?');
            if (qIdx < 0) {
                return s;
            }
            if (newQuery.isEmpty()) {
                return base.substring(0, qIdx);
            }
            return base.substring(0, qIdx + 1) + newQuery;
        } catch (Exception ignored) {
            return maskSensitiveQueryInPathStyle(s);
        }
    }

    private static String maskSensitiveQueryInPathStyle(String s) {
        int q = s.indexOf('?');
        if (q < 0) {
            return s;
        }
        String path = s.substring(0, q);
        String query = s.substring(q + 1);
        String[] pairs = query.split("&");
        StringBuilder qb = new StringBuilder();
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            if (SENSITIVE_QUERY_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (qb.length() > 0) {
                qb.append('&');
            }
            qb.append(pair);
        }
        if (qb.length() == 0) {
            return path;
        }
        return path + "?" + qb;
    }

    private static String urlDecode(String enc) {
        try {
            return URLDecoder.decode(enc, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return enc;
        }
    }
}
