package com.miduo.cloud.ticket.common.constants;

/**
 * Redis Key 常量
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String WECOM_ACCESS_TOKEN = "wecom:access_token";
    public static final String WECOM_CONTACT_ACCESS_TOKEN = "wecom:access_token:contact";
    public static final String WECOM_CALLBACK_MSG_DEDUP_PREFIX = "wecom:callback:msg:dedup:";

    public static final String JWT_REFRESH_TOKEN_PREFIX = "jwt:refresh:";

    public static final String CACHE_CATEGORIES = "cache:categories";
    public static final String CACHE_WORKFLOW_PREFIX = "cache:workflow:";
    public static final String CACHE_SLA_POLICY_PREFIX = "cache:sla_policy:";
    public static final String CACHE_USER_PREFIX = "cache:user:";

    public static final String NOTIFY_DEDUP_PREFIX = "notify:dedup:";
    public static final String NOTIFY_AGGREGATE_PREFIX = "notify:aggregate:";

    public static final String TRACK_READ_PREFIX = "track:read:";

    public static final String WS_USER_SESSION_PREFIX = "ws:user:session:";
    public static final String SLA_CHECK_LOCK = "sla:check:lock";

    /**
     * NLP关键词缓存（TTL 5分钟）
     */
    public static final String WECOM_NLP_KEYWORDS_CACHE = "wecom:nlp:keywords";

    /**
     * 企微工单草稿会话（群聊），格式：wecom:draft:{chatId}:{wecomUserId}
     */
    public static final String WECOM_DRAFT_SESSION_PREFIX = "wecom:draft:";

    /**
     * 企微图片待处理提示去重，格式：wecom:image:notify:{chatId}:{fromUserId}
     */
    public static final String WECOM_IMAGE_NOTIFY_DEDUP_PREFIX = "wecom:image:notify:";
}
