package com.miduo.cloud.ticket.common.constants;

/**
 * IDE/Agent 个人 API 密钥相关常量
 */
public final class AgentApiKeyConstants {

    private AgentApiKeyConstants() {
    }

    /** HTTP 请求头：明文 API Key */
    public static final String HEADER_NAME = "X-Api-Key";

    /** HTTP 标准鉴权头：用于 MCP 等以 {@code Authorization: Bearer <key>} 方式携带密钥的客户端 */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 前缀（含末尾空格） */
    public static final String BEARER_PREFIX = "Bearer ";

    /** 生成密钥时的前缀（便于识别与日志脱敏） */
    public static final String KEY_PREFIX_LABEL = "mdt_";
}
