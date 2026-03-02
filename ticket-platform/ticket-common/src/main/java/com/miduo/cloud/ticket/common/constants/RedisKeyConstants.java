package com.miduo.cloud.ticket.common.constants;

/**
 * Redis Key 常量
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String WECOM_ACCESS_TOKEN = "wecom:access_token";

    public static final String CACHE_CATEGORIES = "cache:categories";
    public static final String CACHE_WORKFLOW_PREFIX = "cache:workflow:";
    public static final String CACHE_SLA_POLICY_PREFIX = "cache:sla_policy:";
    public static final String CACHE_USER_PREFIX = "cache:user:";

    public static final String NOTIFY_DEDUP_PREFIX = "notify:dedup:";
    public static final String NOTIFY_AGGREGATE_PREFIX = "notify:aggregate:";

    public static final String TRACK_READ_PREFIX = "track:read:";
}
