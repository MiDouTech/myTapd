package com.miduo.cloud.ticket.infrastructure.external.wework;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业微信配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "wecom")
public class WecomProperties {

    /**
     * 企业ID（CorpID）
     */
    private String corpId;

    /**
     * 自建应用AgentID
     */
    private String agentId;

    /**
     * 应用Secret
     */
    private String secret;

    /**
     * 通讯录同步Secret
     */
    private String contactSecret;

    /**
     * 回调Token
     */
    private String callbackToken;

    /**
     * 回调EncodingAESKey
     */
    private String callbackAesKey;

    /**
     * 可信域名
     */
    private String trustedDomain;

    /**
     * 企微图片消息处理配置
     */
    private ImageConfig image = new ImageConfig();

    @lombok.Data
    public static class ImageConfig {
        /**
         * 是否启用图片消息处理（默认 true）
         */
        private boolean enabled = true;

        /**
         * 关联时间窗口（分钟，默认 5 分钟，范围 1-60）
         */
        private int associationWindowMinutes = 5;

        /**
         * 超时策略（CREATE_TICKET / EXPIRE / NOTIFY_USER，默认 CREATE_TICKET）
         */
        private String timeoutStrategy = "CREATE_TICKET";

        /**
         * 收到图片时是否立即回复提示（默认 false）
         */
        private boolean notifyOnPending = false;

        /**
         * 单工单最大图片数（默认 10）
         */
        private int maxImagesPerTicket = 10;

        /**
         * 智能机器人视频：加密临时文件不超过该大小时（MB）在内存中解密，超过则落盘分块解密以降低堆占用。
         */
        private int videoDecryptInMemoryMaxMb = 8;

        /**
         * 视频解密/上传临时文件目录；为空则使用 {@code java.io.tmpdir}。
         */
        private String videoTempDirectory = "";
    }
}
