package com.miduo.cloud.ticket.infrastructure.external.qiniu;

import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 七牛云文件上传服务
 */
@Service
public class QiniuUploadService {

    private static final Logger log = LoggerFactory.getLogger(QiniuUploadService.class);

    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    ));

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    ));

    @Resource
    private QiniuProperties qiniuProperties;

    private Auth auth;
    private UploadManager uploadManager;

    @PostConstruct
    public void init() {
        String accessKey = qiniuProperties.getAccessKey();
        String secretKey = qiniuProperties.getSecretKey();
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            log.warn("七牛云 AccessKey 或 SecretKey 未配置，上传服务不可用");
            return;
        }
        auth = Auth.create(accessKey, secretKey);
        Region region = resolveRegion(qiniuProperties.getZone());
        Configuration cfg = new Configuration(region);
        uploadManager = new UploadManager(cfg);
        log.info("七牛云上传服务初始化完成，Bucket={}, Domain={}", qiniuProperties.getBucket(), qiniuProperties.getDomain());
    }

    /**
     * 流式上传媒体文件到七牛云（适用于视频等大文件，避免全量加载到内存）
     *
     * @param stream        媒体文件输入流（方法内部关闭）
     * @param originalName  文件名（用于生成 key 和扩展名）
     * @param contentType   MIME 类型，如 video/mp4
     * @param pathPrefix    七牛存储路径前缀，如 ticket/videos/
     * @return 可访问的文件 URL，上传失败时返回 null
     */
    public String uploadStream(InputStream stream, String originalName, String contentType, String pathPrefix) {
        if (auth == null || uploadManager == null) {
            log.warn("七牛云存储服务未配置，流式上传跳过");
            return null;
        }
        if (stream == null) {
            log.warn("上传流为空，跳过上传");
            return null;
        }
        String key = buildObjectKey(originalName, pathPrefix);
        String token = auth.uploadToken(qiniuProperties.getBucket(), null,
                qiniuProperties.getTokenExpireSeconds(), new StringMap());
        try {
            Response response = uploadManager.put(stream, key, token, null, contentType);
            DefaultPutRet putRet = com.alibaba.fastjson2.JSON.parseObject(response.bodyString(), DefaultPutRet.class);
            if (putRet == null || !StringUtils.hasText(putRet.key)) {
                log.error("七牛云流式上传返回结果异常，key 为空");
                return null;
            }
            String fileUrl = buildAccessUrl(putRet.key);
            log.info("流式上传七牛云成功: key={}, url={}", putRet.key, fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("流式上传七牛云失败: originalName={}, contentType={}", originalName, contentType, e);
            return null;
        } finally {
            try {
                stream.close();
            } catch (Exception ignored) {
                // 关闭流失败不影响主流程
            }
        }
    }

    /**
     * 上传图片字节数组到七牛云（用于企微 MediaId 下载后直接上传）
     *
     * @param imageBytes    图片字节数组
     * @param originalName  文件名（用于生成 key）
     * @return 可访问的图片 URL，上传失败时返回 null
     */
    public String uploadImageBytes(byte[] imageBytes, String originalName) {
        if (auth == null || uploadManager == null) {
            log.warn("七牛云存储服务未配置，图片上传跳过");
            return null;
        }
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("图片字节数组为空，跳过上传");
            return null;
        }
        long maxBytes = qiniuProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (imageBytes.length > maxBytes) {
            log.warn("图片字节超过大小限制 {}MB，跳过上传", qiniuProperties.getMaxFileSizeMb());
            return null;
        }
        String key = buildObjectKey(originalName, qiniuProperties.getImagePathPrefix());
        String token = auth.uploadToken(qiniuProperties.getBucket(), null,
                qiniuProperties.getTokenExpireSeconds(), new StringMap());
        try {
            Response response = uploadManager.put(imageBytes, key, token);
            DefaultPutRet putRet = com.alibaba.fastjson2.JSON.parseObject(response.bodyString(), DefaultPutRet.class);
            if (putRet == null || !StringUtils.hasText(putRet.key)) {
                log.error("七牛云返回结果异常，key 为空");
                return null;
            }
            String imageUrl = buildAccessUrl(putRet.key);
            log.info("企微图片上传七牛云成功: key={}, url={}", putRet.key, imageUrl);
            return imageUrl;
        } catch (Exception e) {
            log.error("企微图片上传七牛云失败: originalName={}", originalName, e);
            return null;
        }
    }

    /**
     * 上传图片文件到七牛云
     *
     * @param file       图片文件
     * @return 可访问的图片 URL
     */
    public String uploadImage(MultipartFile file) {
        validateUploadReady();
        validateImageFile(file);

        String key = buildObjectKey(file.getOriginalFilename(), qiniuProperties.getImagePathPrefix());
        String token = auth.uploadToken(qiniuProperties.getBucket(), null,
                qiniuProperties.getTokenExpireSeconds(), new StringMap());

        try (InputStream inputStream = file.getInputStream()) {
            Response response = uploadManager.put(inputStream, key, token, null, file.getContentType());
            DefaultPutRet putRet = com.alibaba.fastjson2.JSON.parseObject(response.bodyString(), DefaultPutRet.class);
            if (putRet == null || !StringUtils.hasText(putRet.key)) {
                throw BusinessException.of(ErrorCode.UPLOAD_FAILED, "七牛云返回结果异常");
            }
            String imageUrl = buildAccessUrl(putRet.key);
            log.info("图片上传成功: key={}, url={}", putRet.key, imageUrl);
            return imageUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片上传失败: originalFilename={}", file.getOriginalFilename(), e);
            throw BusinessException.of(ErrorCode.UPLOAD_FAILED, "图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 生成指定 Bucket 的上传 Token（供前端直传使用）
     */
    public String generateUploadToken() {
        validateUploadReady();
        return auth.uploadToken(qiniuProperties.getBucket(), null,
                qiniuProperties.getTokenExpireSeconds(), new StringMap());
    }

    private void validateUploadReady() {
        if (auth == null || uploadManager == null) {
            throw BusinessException.of(ErrorCode.UPLOAD_FAILED, "七牛云存储服务未配置，请联系管理员");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }

        long maxBytes = qiniuProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "文件大小超过限制，最大允许 " + qiniuProperties.getMaxFileSizeMb() + "MB");
        }

        String contentType = file.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID,
                    "仅支持上传图片文件（JPG/PNG/GIF/WEBP/BMP）");
        }

        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename)) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID,
                        "仅支持上传图片文件（jpg/jpeg/png/gif/webp/bmp）");
            }
        }
    }

    private String buildObjectKey(String originalFilename, String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ext = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        } else {
            ext = ".jpg";
        }
        return prefix + date + "/" + uuid + ext;
    }

    private String buildAccessUrl(String key) {
        String domain = qiniuProperties.getDomain();
        if (!StringUtils.hasText(domain)) {
            return key;
        }
        if (domain.endsWith("/")) {
            return domain + key;
        }
        return domain + "/" + key;
    }

    private Region resolveRegion(String zone) {
        if (zone == null) {
            return Region.autoRegion();
        }
        switch (zone.toLowerCase()) {
            case "z0":
                return Region.region0();
            case "z1":
                return Region.region1();
            case "z2":
                return Region.region2();
            case "na0":
                return Region.regionNa0();
            case "as0":
                return Region.regionAs0();
            default:
                return Region.autoRegion();
        }
    }
}
