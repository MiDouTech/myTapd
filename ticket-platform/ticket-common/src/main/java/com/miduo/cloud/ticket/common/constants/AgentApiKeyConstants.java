package com.miduo.cloud.ticket.common.constants;

/**
 * IDE/Agent 个人 API 密钥相关常量
 */
public final class AgentApiKeyConstants {

    private AgentApiKeyConstants() {
    }

    /** HTTP 请求头：明文 API Key */
    public static final String HEADER_NAME = "X-Api-Key";

    /** 生成密钥时的前缀（便于识别与日志脱敏） */
    public static final String KEY_PREFIX_LABEL = "mdt_";
}
