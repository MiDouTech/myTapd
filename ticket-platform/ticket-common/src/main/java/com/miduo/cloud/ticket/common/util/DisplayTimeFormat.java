package com.miduo.cloud.ticket.common.util;

import com.miduo.cloud.ticket.common.constants.AppConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 与前端页面、Jackson（spring.jackson.time-zone）一致的展示时区格式化。
 * 避免 SimpleDateFormat 默认使用 JVM 时区（容器常为 UTC）导致与网页差 8 小时。
 */
public final class DisplayTimeFormat {

    /** 与 application.yml 中 spring.jackson.time-zone 及 DTO 上 JsonFormat 默认一致 */
    public static final String TIMEZONE_ID = "Asia/Shanghai";

    private static final TimeZone DISPLAY_TIME_ZONE = TimeZone.getTimeZone(TIMEZONE_ID);

    private DisplayTimeFormat() {
    }

    /**
     * 创建已绑定展示时区的 {@link SimpleDateFormat}（非线程安全，勿静态共享）。
     */
    public static SimpleDateFormat newFormatter(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(DISPLAY_TIME_ZONE);
        return sdf;
    }

    public static String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return newFormatter(AppConstants.DATETIME_FORMAT).format(date);
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return newFormatter(AppConstants.DATE_FORMAT).format(date);
    }
}
