package com.miduo.cloud.ticket.common.util;

import com.miduo.cloud.ticket.common.constants.AppConstants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工单编号生成器
 * 格式: 前缀-YYYYMMDD-序号-随机段
 * 例: WO-20260228-001-4821, BUG-20260228-015-9032
 */
public class TicketNoGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private TicketNoGenerator() {
    }

    public static String generateTicketNo() {
        return generate(AppConstants.TICKET_NO_PREFIX);
    }

    public static String generateBugTicketNo() {
        return generate(AppConstants.BUG_TICKET_NO_PREFIX);
    }

    public static String generateBugReportNo() {
        return generate(AppConstants.BUG_REPORT_NO_PREFIX);
    }

    private static String generate(String prefix) {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        long seq = SEQUENCE.incrementAndGet();
        // 进程重启后序列会归零，增加随机段避免与历史工单号冲突
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);
        return String.format("%s-%s-%03d-%04d", prefix, datePart, seq % 1000, randomPart);
    }
}
