package com.miduo.cloud.ticket.infrastructure.external.wework;

import cn.hutool.crypto.SecureUtil;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企微回调签名校验与消息解密服务
 */
@Component
public class WecomCallbackCryptoService {

    private static final Logger log = LoggerFactory.getLogger(WecomCallbackCryptoService.class);
    private static final int RANDOM_PREFIX_LENGTH = 16;
    private static final int NETWORK_ORDER_LENGTH = 4;
    private static final int PKCS7_BLOCK_SIZE = 32;

    private final WecomProperties wecomProperties;

    public WecomCallbackCryptoService(WecomProperties wecomProperties) {
        this.wecomProperties = wecomProperties;
    }

    /**
     * URL验证：校验签名并解密echostr
     */
    public String verifyAndDecodeEcho(String msgSignature, String timestamp, String nonce, String echostr) {
        validateSignature(msgSignature, timestamp, nonce, echostr);
        return decrypt(echostr);
    }

    /**
     * 消息回调：校验签名并解密消息体
     */
    public String decryptMessage(String msgSignature, String timestamp, String nonce, String encrypted) {
        validateSignature(msgSignature, timestamp, nonce, encrypted);
        return decrypt(encrypted);
    }

    private void validateSignature(String msgSignature, String timestamp, String nonce, String encrypted) {
        String token = wecomProperties.getCallbackToken();
        if (isBlank(token) || isBlank(msgSignature) || isBlank(timestamp) || isBlank(nonce) || isBlank(encrypted)) {
            throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调签名参数不完整");
        }

        String[] values = new String[]{token, timestamp, nonce, encrypted};
        Arrays.sort(values);
        String expected = SecureUtil.sha1(String.join("", values));
        if (!expected.equals(msgSignature)) {
            log.warn("企微回调签名校验失败: expected={}, actual={}", expected, msgSignature);
            throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调签名校验失败");
        }
    }

    private String decrypt(String encryptedText) {
        try {
            byte[] key = decodeAesKey();
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(key, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

            byte[] decrypted = cipher.doFinal(encrypted);
            byte[] unpadded = removePadding(decrypted);
            if (unpadded.length < RANDOM_PREFIX_LENGTH + NETWORK_ORDER_LENGTH) {
                throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调解密内容长度异常");
            }

            byte[] lengthBytes = Arrays.copyOfRange(
                    unpadded, RANDOM_PREFIX_LENGTH, RANDOM_PREFIX_LENGTH + NETWORK_ORDER_LENGTH);
            int xmlLength = recoverNetworkBytesOrder(lengthBytes);

            int xmlStart = RANDOM_PREFIX_LENGTH + NETWORK_ORDER_LENGTH;
            int xmlEnd = xmlStart + xmlLength;
            if (xmlEnd > unpadded.length) {
                throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调解密消息体长度非法");
            }

            String xml = new String(Arrays.copyOfRange(unpadded, xmlStart, xmlEnd), StandardCharsets.UTF_8);
            String receiveId = new String(Arrays.copyOfRange(unpadded, xmlEnd, unpadded.length), StandardCharsets.UTF_8);

            String corpId = wecomProperties.getCorpId();
            if (!isBlank(corpId) && !corpId.equals(receiveId)) {
                throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调接收方校验失败");
            }
            return xml;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("企微回调解密失败", ex);
            throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调解密失败");
        }
    }

    private byte[] decodeAesKey() {
        String callbackAesKey = wecomProperties.getCallbackAesKey();
        if (isBlank(callbackAesKey)) {
            throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "未配置企微回调AESKey");
        }
        byte[] key = Base64.getDecoder().decode(callbackAesKey + "=");
        if (key.length != 32) {
            throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调AESKey长度非法");
        }
        return key;
    }

    private byte[] removePadding(byte[] decrypted) {
        int pad = decrypted[decrypted.length - 1] & 0xFF;
        if (pad < 1 || pad > PKCS7_BLOCK_SIZE) {
            pad = 0;
        }
        int contentLength = decrypted.length - pad;
        return Arrays.copyOfRange(decrypted, 0, contentLength);
    }

    private int recoverNetworkBytesOrder(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
