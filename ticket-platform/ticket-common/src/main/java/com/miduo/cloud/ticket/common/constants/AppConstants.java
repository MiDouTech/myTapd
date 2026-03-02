package com.miduo.cloud.ticket.common.constants;

/**
 * 应用常量
 */
public final class AppConstants {

    private AppConstants() {
    }

    public static final String APP_NAME = "ticket-platform";

    public static final int LOGIC_NOT_DELETED = 0;
    public static final int LOGIC_DELETED = 1;

    public static final int MAX_CATEGORY_LEVEL = 3;

    public static final int MAX_ATTACHMENT_SIZE_MB = 20;

    public static final String TICKET_NO_PREFIX = "WO";
    public static final String BUG_TICKET_NO_PREFIX = "BUG";
    public static final String BUG_REPORT_NO_PREFIX = "BR";

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
