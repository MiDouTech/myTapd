package com.miduo.cloud.ticket.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将前端日期范围等查询条件中的「日期 / 当日零点」规范为 SQL 可用的起止时刻，
 * 避免结束日仅含 00:00:00 时把该日后续创建的记录排除在外。
 */
public final class DateTimeRangeQueryUtil {

    private static final Pattern DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern MIDNIGHT_OF_DAY = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}) 00:00:00$");

    private DateTimeRangeQueryUtil() {
    }

    /**
     * 范围起点：纯日期补全为当天 00:00:00；已带时间的原样返回（去首尾空格）。
     */
    public static String normalizeRangeStart(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (DATE_ONLY.matcher(t).matches()) {
            return t + " 00:00:00";
        }
        return t;
    }

    /**
     * 范围终点（含当天）：纯日期或「yyyy-MM-dd 00:00:00」补全为当天 23:59:59；
     * 其他已带非零点时间的原样返回。
     */
    public static String normalizeRangeEndInclusive(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (DATE_ONLY.matcher(t).matches()) {
            return t + " 23:59:59";
        }
        Matcher m = MIDNIGHT_OF_DAY.matcher(t);
        if (m.matches()) {
            return m.group(1) + " 23:59:59";
        }
        return t;
    }
}
