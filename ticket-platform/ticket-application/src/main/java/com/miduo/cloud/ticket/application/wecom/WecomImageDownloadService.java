package com.miduo.cloud.ticket.application.wecom;

import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuUploadService;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import com.miduo.cloud.ticket.infrastructure.external.wework.WeworkRuntimeConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企微图片下载与持久化服务
 * 接口编号：（内部服务，无对外 REST 接口）
 * <p>
 * 企微智能机器人（aibot）图片消息格式（官方文档 /document/path/100719）：
 *   {"msgtype":"image","image":{"url":"https://ww-aibot-img-...（5分钟有效）"}}
 * <p>
 * 图片内容已使用回调 callbackAesKey 做 AES-256-CBC 加密：
 *   - AES Key  = Base64.decode(callbackAesKey + "=")，32字节
 *   - IV       = AES Key 前 16 字节
 *   - 填充     = PKCS#7，填充至 32 字节倍数
 * 下载后用上述规则解密，再上传到七牛云。
 */
@Service
public class WecomImageDownloadService {

    private static final Logger log = LoggerFactory.getLogger(WecomImageDownloadService.class);

    private static final int AES_KEY_BYTES = 32;
    private static final int IV_BYTES = 16;
    private static final int PKCS7_BLOCK_SIZE = 32;

    private final WecomClient wecomClient;
    private final QiniuUploadService qiniuUploadService;
    private final WeworkRuntimeConfigProvider runtimeConfigProvider;

    public WecomImageDownloadService(WecomClient wecomClient,
                                     QiniuUploadService qiniuUploadService,
                                     WeworkRuntimeConfigProvider runtimeConfigProvider) {
        this.wecomClient = wecomClient;
        this.qiniuUploadService = qiniuUploadService;
        this.runtimeConfigProvider = runtimeConfigProvider;
    }

    /**
     * 下载企微图片并上传到七牛云，返回持久化 URL。
     * 兼容两种来源：
     *   1. 智能机器人（aibot）图片：imageUrl = image.url，需用 callbackAesKey 解密
     *   2. 普通应用 MediaId / PicUrl（保留降级能力）
     *
     * @param mediaId   企微 MediaId（普通应用场景，可为空）
     * @param picUrl    企微临时预览 URL（可为空）
     * @param imageUrl  企微 aibot 图片加密下载 URL（来自 image.url，可为空）
     * @param msgId     消息 ID（用于日志）
     * @return 七牛云持久化 URL，全部失败时返回 null
     */
    public String downloadAndUpload(String mediaId, String picUrl, String imageUrl, String msgId) {
        log.info("企微图片下载开始: msgId={}, hasImageUrl={}, hasMediaId={}, hasPicUrl={}",
                msgId, isNotBlank(imageUrl), isNotBlank(mediaId), isNotBlank(picUrl));

        if (!isNotBlank(imageUrl) && !isNotBlank(mediaId) && !isNotBlank(picUrl)) {
            log.error("企微图片下载字段全部为空，无法下载: msgId={}", msgId);
            return null;
        }

        byte[] imageBytes = null;
        String downloadSource = null;

        // 策略1：aibot 加密图片 URL（主路径）
        if (isNotBlank(imageUrl)) {
            imageBytes = downloadAndDecryptAibot(imageUrl, msgId);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "AibotImageUrl";
                log.info("企微aibot图片解密下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微aibot图片解密下载失败，降级MediaId: msgId={}", msgId);
            }
        }

        // 策略2：普通应用 MediaId API
        if (imageBytes == null && isNotBlank(mediaId)) {
            imageBytes = wecomClient.downloadMediaById(mediaId);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "MediaId";
                log.info("企微图片MediaId下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微图片MediaId下载失败，降级PicUrl: msgId={}, mediaId={}", msgId, mediaId);
            }
        }

        // 策略3：PicUrl HTTP 直链
        if (imageBytes == null && isNotBlank(picUrl)) {
            imageBytes = wecomClient.downloadImageByUrl(picUrl);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "PicUrl";
                log.info("企微图片PicUrl下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微图片PicUrl下载也失败: msgId={}, picUrl={}", msgId, picUrl);
            }
        }

        if (imageBytes == null || imageBytes.length == 0) {
            log.error("企微图片下载全部失败，放弃上传: msgId={}", msgId);
            return null;
        }

        String originalName = "wecom_" + msgId + ".jpg";
        String qiniuUrl = qiniuUploadService.uploadImageBytes(imageBytes, originalName);
        if (qiniuUrl == null) {
            log.error("企微图片上传七牛云失败: msgId={}, downloadSource={}", msgId, downloadSource);
            return null;
        }

        log.info("企微图片下载并上传成功: msgId={}, downloadSource={}, qiniuUrl={}", msgId, downloadSource, qiniuUrl);
        return qiniuUrl;
    }

    /**
     * 兼容旧签名：mediaId + picUrl + downloadUrl(imageUrl) + aesKey(已废弃，来自消息字段不再使用)
     * AES Key 统一从 callbackAesKey 配置读取。
     */
    public String downloadAndUpload(String mediaId, String picUrl, String imageUrl, String ignoredAesKey, String msgId) {
        return downloadAndUpload(mediaId, picUrl, imageUrl, msgId);
    }

    /**
     * 下载并解密企微 aibot 图片。
     * <p>
     * 加密规则（官方文档 /document/path/100719）：
     *   AES-256-CBC，PKCS#7填充至32字节倍数，IV = AESKey前16字节，
     *   AESKey = Base64.decode(callbackAesKey + "=")
     */
    private byte[] downloadAndDecryptAibot(String imageUrl, String msgId) {
        try {
            // 1. 获取回调 AES 密钥
            byte[] keyBytes = decodeCallbackAesKey();

            // 2. 下载加密字节
            byte[] encryptedBytes = cn.hutool.http.HttpUtil.downloadBytes(imageUrl.trim());
            if (encryptedBytes == null || encryptedBytes.length == 0) {
                log.warn("企微aibot图片URL下载返回空数据: msgId={}", msgId);
                return null;
            }

            // 3. AES-256-CBC 解密，NoPadding + 手动 PKCS7 去除（填充至32字节倍数）
            byte[] iv = Arrays.copyOfRange(keyBytes, 0, IV_BYTES);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(encryptedBytes);

            // 4. 去除 PKCS#7 填充（块大小32）
            return removePkcs7Padding(decrypted, msgId);

        } catch (Exception e) {
            log.warn("企微aibot图片解密失败: msgId={}, error={}", msgId, e.getMessage());
            return null;
        }
    }

    /**
     * 从配置读取并解码回调 AES 密钥（32字节）。
     * 与 WecomCallbackCryptoService.decodeAesKey() 使用相同逻辑。
     */
    private byte[] decodeCallbackAesKey() {
        String callbackAesKey = runtimeConfigProvider.getRuntimeConfig().getCallbackAesKey();
        if (callbackAesKey == null || callbackAesKey.trim().isEmpty()) {
            throw new IllegalStateException("企微回调AESKey未配置，无法解密图片");
        }
        byte[] key = Base64.getDecoder().decode(callbackAesKey.trim() + "=");
        if (key.length != AES_KEY_BYTES) {
            throw new IllegalStateException("企微回调AESKey解码长度非法: " + key.length + "字节，期望32字节");
        }
        return key;
    }

    /**
     * 去除 PKCS#7 填充（块大小 PKCS7_BLOCK_SIZE=32）。
     */
    private byte[] removePkcs7Padding(byte[] data, String msgId) {
        if (data == null || data.length == 0) {
            return data;
        }
        int pad = data[data.length - 1] & 0xFF;
        if (pad < 1 || pad > PKCS7_BLOCK_SIZE) {
            log.warn("企微aibot图片解密：填充字节值非法（{}），按原始数据返回: msgId={}", pad, msgId);
            return data;
        }
        for (int i = data.length - pad; i < data.length; i++) {
            if ((data[i] & 0xFF) != pad) {
                log.warn("企微aibot图片解密：填充字节内容不一致，按原始数据返回: msgId={}", msgId);
                return data;
            }
        }
        return Arrays.copyOfRange(data, 0, data.length - pad);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
