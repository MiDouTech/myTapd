package com.miduo.cloud.ticket.application.wecom;

import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuUploadService;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 企微图片下载与持久化服务
 * 接口编号：（内部服务，无对外 REST 接口）
 * 策略：优先 MediaId API 下载，失败时降级 PicUrl HTTP 下载，下载成功后上传到七牛云
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
        byte[] imageBytes = null;
        String downloadSource = null;

        if (mediaId != null && !mediaId.trim().isEmpty()) {
            imageBytes = wecomClient.downloadMediaById(mediaId);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "MediaId";
                log.info("企微图片MediaId下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微图片MediaId下载失败，降级PicUrl: msgId={}, mediaId={}", msgId, mediaId);
            }
        }

        if (imageBytes == null && picUrl != null && !picUrl.trim().isEmpty()) {
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
}
