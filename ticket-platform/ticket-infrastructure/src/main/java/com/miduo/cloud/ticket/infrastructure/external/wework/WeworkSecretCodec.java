package com.miduo.cloud.ticket.infrastructure.external.wework;

import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企业微信密钥编解码器
 */
@Component
public class WeworkSecretCodec {

    private static final String PREFIX = "ENC:";

    @Value("${wecom.config-secret-key:${jwt.secret:}}")
    private String secretKey;

    public String encode(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return plainText;
        }
        try {
            byte[] encrypted = buildCipher(Cipher.ENCRYPT_MODE).doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "企微密钥加密失败");
        }
    }

    public String decode(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            return encoded;
        }
        if (!encoded.startsWith(PREFIX)) {
            return encoded;
        }
        try {
            String cipherText = encoded.substring(PREFIX.length());
            byte[] plain = buildCipher(Cipher.DECRYPT_MODE).doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "企微密钥解密失败");
        }
    }

    private Cipher buildCipher(int mode) throws Exception {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "未配置wecom.config-secret-key");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fullKey = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        byte[] aesKey = Arrays.copyOf(fullKey, 16);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(mode, keySpec);
        return cipher;
    }
}
