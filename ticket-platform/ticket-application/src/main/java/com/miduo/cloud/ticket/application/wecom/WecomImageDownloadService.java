package com.miduo.cloud.ticket.application.wecom;

import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuUploadService;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 企微图片下载与持久化服务
 * 接口编号：（内部服务，无对外 REST 接口）
 * 策略：
 *   1. AI bot 单聊图片：优先使用 download_url + aes_key（AES-256-CBC 解密），
 *      失败时降级普通 MediaId API
 *   2. 普通群聊图片：优先 MediaId API，失败时降级 PicUrl HTTP 下载
 *   下载成功后上传到七牛云
 */
@Service
public class WecomImageDownloadService {

    private static final Logger log = LoggerFactory.getLogger(WecomImageDownloadService.class);

    private final WecomClient wecomClient;
    private final QiniuUploadService qiniuUploadService;

    public WecomImageDownloadService(WecomClient wecomClient, QiniuUploadService qiniuUploadService) {
        this.wecomClient = wecomClient;
        this.qiniuUploadService = qiniuUploadService;
    }

    /**
     * 下载企微图片并上传到七牛云，返回持久化 URL
     * 优先 MediaId API，失败时降级 PicUrl
     *
     * @param mediaId 企微 MediaId（可为空）
     * @param picUrl  企微图片临时预览 URL（可为空）
     * @param msgId   消息 ID（用于日志）
     * @return 七牛云持久化 URL，全部失败时返回 null
     */
    public String downloadAndUpload(String mediaId, String picUrl, String msgId) {
        return downloadAndUpload(mediaId, picUrl, null, null, msgId);
    }

    /**
     * 下载企微图片并上传到七牛云，返回持久化 URL
     * AI bot 图片优先使用 downloadUrl + aesKey（AES-256-CBC 解密），
     * 普通图片使用 MediaId API，均失败时降级 PicUrl HTTP 下载
     *
     * @param mediaId     企微 MediaId（可为空）
     * @param picUrl      企微图片临时预览 URL（可为空）
     * @param downloadUrl 企微 AI bot 图片下载 URL（可为空）
     * @param aesKey      企微 AI bot 图片 AES 密钥（Base64，可为空）
     * @param msgId       消息 ID（用于日志）
     * @return 七牛云持久化 URL，全部失败时返回 null
     */
    public String downloadAndUpload(String mediaId, String picUrl, String downloadUrl, String aesKey, String msgId) {
        byte[] imageBytes = null;
        String downloadSource = null;

        // 优先尝试 AI bot download_url + aes_key 解密下载
        if (isNotBlank(downloadUrl) && isNotBlank(aesKey)) {
            imageBytes = downloadAndDecryptAibot(downloadUrl, aesKey, msgId);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "AibotDownloadUrl";
                log.info("企微AI bot图片解密下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微AI bot图片解密下载失败，降级MediaId: msgId={}", msgId);
            }
        }

        // 降级尝试 MediaId API
        if (imageBytes == null && isNotBlank(mediaId)) {
            imageBytes = wecomClient.downloadMediaById(mediaId);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "MediaId";
                log.info("企微图片MediaId下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微图片MediaId下载失败，降级PicUrl: msgId={}, mediaId={}", msgId, mediaId);
            }
        }

        // 最终降级 PicUrl
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
     * 下载 AI bot 加密图片并 AES-256-CBC 解密
     * 企微 AI bot 图片使用 AES-256-CBC 加密，iv 为密钥前16字节
     *
     * @param downloadUrl AI bot 图片下载 URL
     * @param aesKey      Base64 编码的 AES 密钥（32 字节解码后）
     * @param msgId       消息 ID（用于日志）
     * @return 解密后的图片字节数组，失败时返回 null
     */
    private byte[] downloadAndDecryptAibot(String downloadUrl, String aesKey, String msgId) {
        try {
            byte[] encryptedBytes = cn.hutool.http.HttpUtil.downloadBytes(downloadUrl.trim());
            if (encryptedBytes == null || encryptedBytes.length == 0) {
                log.warn("企微AI bot图片URL下载返回空数据: msgId={}", msgId);
                return null;
            }
            byte[] keyBytes = Base64.getDecoder().decode(aesKey.trim());
            // AES-256-CBC：key 为全部 32 字节，iv 为 key 前 16 字节
            byte[] iv = new byte[16];
            System.arraycopy(keyBytes, 0, iv, 0, 16);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            return cipher.doFinal(encryptedBytes);
        } catch (Exception e) {
            log.warn("企微AI bot图片解密失败: msgId={}, error={}", msgId, e.getMessage());
            return null;
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
