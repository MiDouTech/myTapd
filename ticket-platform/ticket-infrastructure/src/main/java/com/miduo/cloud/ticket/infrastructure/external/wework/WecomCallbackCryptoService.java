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
     * 企微智能机器人注册时会发送 GET 请求，此处解密后直接返回明文 echostr（纯字符串）
     */
    public String verifyAndDecodeEcho(String msgSignature, String timestamp, String nonce, String echostr) {
        log.info("企微URL验证开始: timestamp={}, nonce={}, msgSignature={}, echostr长度={}",
                timestamp, nonce, msgSignature, echostr == null ? 0 : echostr.length());
        validateSignature(msgSignature, timestamp, nonce, echostr);
        String result = decrypt(echostr);
        log.info("企微URL验证成功，返回明文echostr长度={}", result == null ? 0 : result.length());
        return result;
    }

    /**
     * 消息回调：校验签名并解密消息体
     */
    public String decryptMessage(String msgSignature, String timestamp, String nonce, String encrypted) {
        log.debug("企微消息解密开始: timestamp={}, nonce={}", timestamp, nonce);
        validateSignature(msgSignature, timestamp, nonce, encrypted);
        String result = decrypt(encrypted);
        log.debug("企微消息解密成功，xml长度={}", result == null ? 0 : result.length());
        return result;
    }

    private void validateSignature(String msgSignature, String timestamp, String nonce, String encrypted) {
        String token = wecomProperties.getCallbackToken();
        log.debug("企微签名校验: token配置={}, msgSignature={}, timestamp={}, nonce={}, encrypted长度={}",
                isBlank(token) ? "未配置" : "已配置",
                msgSignature, timestamp, nonce, encrypted == null ? 0 : encrypted.length());

        if (isBlank(token)) {
            log.error("企微回调Token未配置，请检查环境变量 WECOM_CALLBACK_TOKEN 是否已设置");
            throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调Token未配置，请联系管理员配置WECOM_CALLBACK_TOKEN");
        }

        if (isBlank(msgSignature) || isBlank(timestamp) || isBlank(nonce) || isBlank(encrypted)) {
            log.error("企微回调请求参数不完整: msgSignature为空={}, timestamp为空={}, nonce为空={}, encrypted为空={}",
                    isBlank(msgSignature), isBlank(timestamp), isBlank(nonce), isBlank(encrypted));
            throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调签名参数不完整");
        }

        String[] values = new String[]{token, timestamp, nonce, encrypted};
        Arrays.sort(values);
        String expected = SecureUtil.sha1(String.join("", values));
        if (!expected.equals(msgSignature)) {
            log.warn("企微回调签名校验失败: expected={}, actual={}", expected, msgSignature);
            throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调签名校验失败");
        }
        log.debug("企微签名校验通过");
    }

    private String decrypt(String encryptedText) {
        try {
            byte[] key = decodeAesKey();
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);
            log.debug("企微AES解密: 密文长度={}", encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(key, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

            byte[] decrypted = cipher.doFinal(encrypted);
            byte[] unpadded = removePadding(decrypted);
            if (unpadded.length < RANDOM_PREFIX_LENGTH + NETWORK_ORDER_LENGTH) {
                log.error("企微回调解密内容长度异常: unpaddedLength={}", unpadded.length);
                throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调解密内容长度异常");
            }

            byte[] lengthBytes = Arrays.copyOfRange(
                    unpadded, RANDOM_PREFIX_LENGTH, RANDOM_PREFIX_LENGTH + NETWORK_ORDER_LENGTH);
            int xmlLength = recoverNetworkBytesOrder(lengthBytes);

            int xmlStart = RANDOM_PREFIX_LENGTH + NETWORK_ORDER_LENGTH;
            int xmlEnd = xmlStart + xmlLength;
            if (xmlEnd > unpadded.length) {
                log.error("企微回调解密消息体长度非法: xmlLength={}, unpaddedLength={}", xmlLength, unpadded.length);
                throw BusinessException.of(ErrorCode.WECOM_CALLBACK_VERIFY_FAILED, "企微回调解密消息体长度非法");
            }

            String xml = new String(Arrays.copyOfRange(unpadded, xmlStart, xmlEnd), StandardCharsets.UTF_8);
            String receiveId = new String(Arrays.copyOfRange(unpadded, xmlEnd, unpadded.length), StandardCharsets.UTF_8);
            log.debug("企微解密完成: receiveId={}, xml长度={}", receiveId, xml.length());

            // 企微智能机器人回调时，receiveId 是机器人 key（如 bot_xxx），而非企业 corpId。
            // 因此此处只记录 warn 日志，不强制抛出异常，避免机器人注册/消息推送被拦截。
            String corpId = wecomProperties.getCorpId();
            if (!isBlank(corpId) && !corpId.equals(receiveId)) {
                log.warn("企微回调 receiveId 与 corpId 不匹配（机器人场景正常）: corpId={}, receiveId={}",
                        corpId, receiveId);
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
