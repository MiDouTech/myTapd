package com.miduo.cloud.ticket.common.constants;

/**
 * 开放接口鉴权常量
 */
public final class OpenApiAuthConstants {

    private OpenApiAuthConstants() {
    }

    public static final String HEADER_APP_KEY = "X-App-Key";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";

    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String SIGN_JOINER = "\n";
}
